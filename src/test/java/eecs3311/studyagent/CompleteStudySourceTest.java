package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompleteStudySourceTest {
    @TempDir Path root;
    private Course course(AgentController agent) throws Exception {
        var semester=agent.createSemester("Fall",LocalDate.of(2026,9,1),LocalDate.of(2026,12,31));
        return agent.createCourse(semester.id(),"EECS3311","#3366CC",Difficulty.MEDIUM);
    }
    private MaterialFile upload(AgentController agent,Course course,String name,String text,Category category) throws Exception {
        Path path=root.resolve(name);Files.writeString(path,text);
        return agent.uploadMaterial(course.id(),category,path);
    }
    private String quiz() { return "{\"questions\":[{\"topic\":\"Contracts\",\"question\":\"When must a precondition hold?\",\"choices\":[\"Before the call\",\"Only after the call\",\"Never\",\"Only at shutdown\"],\"answerIndex\":0,\"explanation\":\"The caller must establish it.\"}]}"; }

    @Test void wholeFileQuizAndPersistentIndexIncludeContentBeyondTheOldPrefix() throws Exception {
        var agent=new AgentController(root.resolve("data"),(s,u)->{
            assertTrue(u.contains("TAIL_PRECONDITION_CALLER"));return quiz();
        });
        var c=course(agent);String full="Introduction ".repeat(2700)+"TAIL_PRECONDITION_CALLER";
        var file=upload(agent,c,"long-lecture.txt",full,Category.LECTURE_MATERIALS);
        agent.prepareMaterials(List.of(file.id()),m->{});
        var saved=Json.object(Json.parse(Files.readString(root.resolve("data/artifacts/source-pages/"+file.id()+".json"))));
        SourceIndex index=SourceIndex.decode(saved);
        assertEquals(full,index.pages().get(0));assertTrue(index.terms().containsKey("tail"));
        assertEquals(1,agent.generateQuiz(file.id(),1,Difficulty.LOW).questions().size());
    }
    @Test void syllabusExtractionIncludesTheLastSectionPastThirtyThousandCharacters() throws Exception {
        StringBuilder seen=new StringBuilder();
        var agent=new AgentController(root.resolve("data"),(s,u)->{
            seen.append(u);
            return u.contains("END_ASSESSMENT") ? "{\"deadlines\":[{\"title\":\"Final Exam\",\"type\":\"Exam\",\"dueDate\":null,\"scope\":\"Contracts\",\"evidence\":\"END_ASSESSMENT\",\"weight\":40}]}" : "{\"deadlines\":[]}";
        });
        var c=course(agent);var file=upload(agent,c,"syllabus.txt","Policies ".repeat(5000)+"END_ASSESSMENT",Category.SYLLABUS);
        assertEquals(1,agent.suggestSyllabusDeadlines(file.id()).size());assertTrue(seen.toString().contains("END_ASSESSMENT"));
    }
    @Test void badCitationIsRegeneratedInsteadOfDisplayedOrSilentlyAccepted() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        var agent=new AgentController(root.resolve("data"),(s,u)-> {
            int page=calls.incrementAndGet()==1?99:1;
            return "{\"answer\":\"A precondition is checked before the call.\",\"citations\":[{\"fileName\":\"contract.txt\",\"page\":"+page+"}]}";
        });
        var c=course(agent);var file=upload(agent,c,"contract.txt","Precondition holds before the call.",Category.LECTURE_MATERIALS);
        assertEquals(1,agent.answerFromMaterial(file.id(),"Explain preconditions").citations().get(0).page());
        assertEquals(2,calls.get());assertEquals(1,agent.history().stream().filter(e->e.event().equals("question answered")).count());
    }
    @Test void assessmentQuestionSeesSyllabusAndApprovedScopeWithoutOldAnswerCitations() throws Exception {
        var agent=new AgentController(root.resolve("data"),(s,u)-> {
            assertTrue(u.contains("Lab Test 2"));assertTrue(u.contains("DECLARED_SCOPE_CONTRACTS"));
            assertFalse(u.contains("GHOST_CITATION"));
            if(s.startsWith("Select uploaded lecture sources"))
                return "{\"scope\":\"Contracts\",\"fileNames\":[\"architecture.txt\"]}";
            return "{\"answer\":\"The syllabus identifies Lab Test 2; review the supplied lecture.\",\"citations\":[{\"fileName\":\"syllabus.txt\",\"page\":1},{\"fileName\":\"architecture.txt\",\"page\":1}]}";
        });
        var c=course(agent);upload(agent,c,"architecture.txt","Software architecture concepts",Category.LECTURE_MATERIALS);
        upload(agent,c,"syllabus.txt","Lab Test 2: November 18, weight 8%.",Category.SYLLABUS);
        agent.addDeadline(c.id(),"Lab Test 2","Test",LocalDate.of(2026,11,18),"DECLARED_SCOPE_CONTRACTS");
        new InteractionHistory(root.resolve("data")).add("question answered","Question: old\nAnswer: GHOST_CITATION p.99");
        assertFalse(agent.answerQuestion(c.id(),"how do i study for labtest 2?").answer().isBlank());
    }
    @Test void imageOnlyPdfIsReadAtPreparationAndItsVisionTextSurvivesRestartWithoutAnotherCall() throws Exception {
        AtomicInteger images=new AtomicInteger();
        LlmClient client=new LlmClient() {
            public String complete(String s,String u) { assertTrue(u.contains("VISUAL_PRECONDITION"));return quiz(); }
            public boolean supportsImages(){return true;}
            public String completeWithImages(String s,String u,List<String> urls) {
                images.incrementAndGet();assertTrue(urls.get(0).startsWith("data:image/png;base64,"));
                return "{\"pages\":[{\"page\":1,\"text\":\"VISUAL_PRECONDITION: the caller establishes the precondition before the method call.\"}]}";
            }
        };
        var agent=new AgentController(root.resolve("data"),client);var c=course(agent);
        Path pdf=root.resolve("image-only.pdf");
        try(var doc=new org.apache.pdfbox.pdmodel.PDDocument()){
            var page=new org.apache.pdfbox.pdmodel.PDPage();doc.addPage(page);
            var image=new java.awt.image.BufferedImage(650,400,java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,650,400);g.setColor(java.awt.Color.BLACK);g.drawString("VISUAL PRECONDITION",20,40);g.dispose();
            var picture=org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc,image);
            try(var out=new org.apache.pdfbox.pdmodel.PDPageContentStream(doc,page)){out.drawImage(picture,20,300,550,350);}
            doc.save(pdf.toFile());
        }
        var file=agent.uploadMaterial(c.id(),Category.LECTURE_MATERIALS,pdf);
        assertFalse(file.textAvailable());agent.prepareMaterials(List.of(file.id()),m->{});
        assertEquals(1,images.get());
        var reopened=new AgentController(root.resolve("data"),client);
        assertEquals(1,reopened.generateQuiz(file.id(),1,Difficulty.LOW).questions().size());assertEquals(1,images.get());
        reopened.clearAllData();assertFalse(Files.exists(root.resolve("data/artifacts/source-pages/"+file.id()+".json")));
    }
    @Test void courseChatIsTheDefaultAndMarkdownFormattingIsRendered() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(()-> {
            var chat=new ChatPanel(s->{},s->{});chat.setFileContext("EECS3311","lecture.pdf");assertFalse(chat.fileOnly());
            chat.useSelectedFile();assertTrue(chat.fileOnly());chat.setContext("EECS2030");assertFalse(chat.fileOnly());
            var text=new ReadableText();text.setMarkdown("## Study plan\n1. **Review contracts** and `requires`.");
            assertFalse(text.getText().contains("**"));assertFalse(text.getText().contains("##"));
            int pos=text.getText().indexOf("Review contracts");
            assertTrue(javax.swing.text.StyleConstants.isBold(text.getStyledDocument().getCharacterElement(pos).getAttributes()));
        });
    }
    @Test void pptxEmbeddedPictureAndAllSlideTextArePersistedTogether() throws Exception {
        AtomicInteger images=new AtomicInteger();
        var client=new LlmClient() {
            public boolean supportsImages(){return true;}
            public String complete(String s,String u){return quiz();}
            public String completeWithImages(String s,String u,List<String> urls){
                images.incrementAndGet();assertEquals(1,urls.size());
                return "{\"pages\":[{\"page\":1,\"text\":\"IMAGE_CONTRACT_RULE\"}]}";
            }
        };
        var agent=new AgentController(root.resolve("data"),client);var c=course(agent);
        Path pptx=root.resolve("slides.pptx");
        var picture=new java.awt.image.BufferedImage(300,300,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(picture,"png",bytes);
        try(var zip=new java.util.zip.ZipOutputStream(Files.newOutputStream(pptx))) {
            Map<String,byte[]> entries=Map.of(
                "ppt/slides/slide1.xml","<p:sld xmlns:p='p' xmlns:a='a'><a:p><a:t>ORIGINAL_SLIDE_TEXT</a:t></a:p></p:sld>".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "ppt/slides/_rels/slide1.xml.rels","<Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/image' Target='../media/picture.png'/></Relationships>".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "ppt/media/picture.png",bytes.toByteArray());
            for(var entry:entries.entrySet()){zip.putNextEntry(new java.util.zip.ZipEntry(entry.getKey()));zip.write(entry.getValue());zip.closeEntry();}
        }
        var file=agent.uploadMaterial(c.id(),Category.LECTURE_MATERIALS,pptx);agent.prepareMaterials(List.of(file.id()),m->{});
        String text=Files.readString(root.resolve("data/artifacts/source-pages/"+file.id()+".txt"));
        assertTrue(text.contains("ORIGINAL_SLIDE_TEXT"));assertTrue(text.contains("IMAGE_CONTRACT_RULE"));assertEquals(1,images.get());
    }
    @Test void cliCanIndexExistingFilesAndRecoverFromAnUnassignedWeek() throws Exception {
        var agent=new AgentController(root.resolve("data"),(s,u)->quiz());var c=course(agent);
        var file=upload(agent,c,"lecture.txt","A caller establishes a precondition.",Category.LECTURE_MATERIALS);
        var input=System.in;var output=System.out;var transcript=new java.io.ByteArrayOutputStream();
        try {
            System.setIn(new java.io.ByteArrayInputStream("material index\nquiz create\nweek\n3\ny\n1\n2\nmcq\nexit\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            System.setOut(new java.io.PrintStream(transcript,true,java.nio.charset.StandardCharsets.UTF_8));
            new CommandLineInterface(agent).run();
        } finally {System.setIn(input);System.setOut(output);}
        assertTrue(transcript.toString(java.nio.charset.StandardCharsets.UTF_8).contains("Indexed 1 complete pages"));
        assertEquals(1,agent.quizzes(c.id()).size());
        assertNull(agent.materials().get(0).lectureWeek(),"Recovery must not invent a week assignment");
    }
    @Test void persistentBadCitationsNeverBecomeASavedAnswer() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        var agent=new AgentController(root.resolve("data"),(s,u)->{
            calls.incrementAndGet();return "{\"answer\":\"unsupported\",\"citations\":[{\"fileName\":\"ghost.txt\",\"page\":99}]}";
        });
        var c=course(agent);var file=upload(agent,c,"lecture.txt","Preconditions hold before the call.",Category.LECTURE_MATERIALS);
        assertThrows(IllegalArgumentException.class,()->agent.answerFromMaterial(file.id(),"Explain preconditions"));
        assertEquals(3,calls.get());assertTrue(agent.history().stream().noneMatch(e->e.event().equals("question answered")));
    }
}
