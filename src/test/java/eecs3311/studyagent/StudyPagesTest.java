package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudyPagesTest {
    @TempDir Path root;
    @Test void pageRangesRejectInvalidAndKeepOnlyRequestedSlides() {
        assertEquals(List.of(2,3,4,8),StudyPages.parse("2-4,8,3",10));
        for(String bad:List.of("0","4-2","11","","1,","1-2-3"))
            assertThrows(IllegalArgumentException.class,()->StudyPages.parse(bad,10));
        String context=StudyPages.selected(List.of("HIDDEN_ONE","VISIBLE_TWO","HIDDEN_THREE"),List.of(2));
        assertTrue(context.contains("[Page 2]"));assertTrue(context.contains("VISIBLE_TWO"));
        assertFalse(context.contains("HIDDEN"));
        assertThrows(IllegalArgumentException.class,()->StudyPages.selected(List.of(""),List.of(1)));
    }
    @Test void sourceBatchesPreserveEveryCharacterAndOriginalPageNumbers() {
        var pages=List.of("START "+"A".repeat(150),"MIDDLE "+"B".repeat(170),"END");
        var batches=StudyPages.batches(pages,64);
        assertTrue(batches.stream().allMatch(s->s.length()<=64));
        String combined=String.join("",batches);
        assertTrue(combined.contains("[Page 1]"));assertTrue(combined.contains("[Page 2]"));assertTrue(combined.contains("[Page 3]"));
        assertEquals(String.join("",pages),combined.replaceAll("\\n\\[Page \\d+\\]\\n",""));
    }
    @Test void slideQuizSendsOnlySelectedPageAndPersistsRealQuestions() throws Exception {
        AtomicReference<String> prompt=new AtomicReference<>();
        AgentController agent=new AgentController(root.resolve("data"),(system,user)-> {
            prompt.set(user);
            return "{\"questions\":[{\"topic\":\"Selected slide\",\"question\":\"What is shown?\",\"choices\":[\"Two\",\"One\",\"Three\",\"Four\"],\"answerIndex\":0,\"explanation\":\"Slide two.\"}]}";
        });
        var sem=agent.createSemester("Fall",LocalDate.of(2026,9,1),LocalDate.of(2026,12,31));
        var course=agent.createCourse(sem.id(),"EECS3311","#3366CC",Difficulty.MEDIUM);
        Path source=root.resolve("slides.txt");Files.writeString(source,"page one\fSELECTED page two\fpage three");
        var file=agent.uploadMaterial(course.id(),Category.LECTURE_MATERIALS,source);
        // TXT is one document page. Use the synthetic PDF to check true physical slide boundaries.
        Path pdf=root.resolve("slides.pdf");
        try(var doc=new org.apache.pdfbox.pdmodel.PDDocument()) {
            for(String value:List.of("FORBIDDEN_ONE","SELECTED_TWO","FORBIDDEN_THREE")) {
                var page=new org.apache.pdfbox.pdmodel.PDPage();doc.addPage(page);
                try(var out=new org.apache.pdfbox.pdmodel.PDPageContentStream(doc,page)) {
                    out.beginText();out.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),12);
                    out.newLineAtOffset(50,700);out.showText(value);out.endText();
                }
            }
            doc.save(pdf.toFile());
        }
        file=agent.uploadMaterial(course.id(),Category.LECTURE_MATERIALS,pdf);
        var quiz=agent.generateSlideQuiz(file.id(),List.of(2),1,Difficulty.LOW);
        assertTrue(prompt.get().contains("SELECTED_TWO"));assertFalse(prompt.get().contains("FORBIDDEN_"));
        assertEquals(1,quiz.questions().size());assertEquals(1,agent.quizzes(course.id()).size());
    }
    @Test void greetingsAndFailuresAreVisibleWithoutLeakingCredentials() {
        assertNotNull(AiFeedback.greeting("Hi!"));assertNull(AiFeedback.greeting("Explain SOLID"));
        assertFalse(AiFeedback.failure(new IllegalArgumentException("invalid sk-abc123XYZ" )).contains("sk-"));
        assertTrue(AiFeedback.failure(new java.net.http.HttpTimeoutException("timeout")).contains("timed out"));
    }

    @Test void fileQuestionCannotSilentlySwitchToAnotherUploadedLecture() throws Exception {
        AgentController agent=new AgentController(root.resolve("data"),(system,user)-> {
            assertTrue(user.contains("VISIBLE_FACTORY"));assertFalse(user.contains("UNRELATED_SECRET_TOPIC"));
            return "{\"answer\":\"The selected lecture explains factories.\",\"citations\":[{\"fileName\":\"factory.txt\",\"page\":1}]}";
        });
        var sem=agent.createSemester("Fall",LocalDate.of(2026,9,1),LocalDate.of(2026,12,31));
        var course=agent.createCourse(sem.id(),"EECS3311","#3366CC",Difficulty.MEDIUM);
        Path source=root.resolve("factory.txt");Files.writeString(source,"VISIBLE_FACTORY explains a factory.");
        var file=agent.uploadMaterial(course.id(),Category.LECTURE_MATERIALS,source);
        Path other=root.resolve("other.txt");Files.writeString(other,"UNRELATED_SECRET_TOPIC");
        agent.uploadMaterial(course.id(),Category.LECTURE_MATERIALS,other);
        assertEquals("factory.txt",agent.answerFromMaterial(file.id(),"Explain a factory").citations().get(0).fileName());
    }
}
