package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AiExceptionCaseTest {
    @TempDir Path root;
    @Test void httpFailuresNeverExposeProviderBodiesOrCredentials() {
        for (int status : List.of(400,401,403,413,429,500,503)) {
            IOException error=assertThrows(IOException.class,()->HttpLlmClient.responseContent(status,"private-provider-body"));
            assertTrue(error.getMessage().contains("HTTP "+status));
            assertFalse(error.getMessage().contains("private-provider-body"));
        }
    }
    @Test void malformedEmptyAndTruncatedResponsesProduceReadableFailures() {
        for(String body:List.of("not json","{}","{\"choices\":[]}",
                "{\"choices\":[{\"message\":{\"content\":\"\"}}]}",
                "{\"choices\":[{\"message\":{\"refusal\":\"Cannot answer\"}}]}",
                "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{\"}}]}")) {
            assertThrows(IOException.class,()->HttpLlmClient.responseContent(200,body));
        }
        assertEquals("{}", assertDoesNotThrow(()->HttpLlmClient.responseContent(200,
                "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{}\"}}]}")));
    }
    @Test void timeoutWrappedFailureAndSecretsHaveSafeFeedback() {
        assertTrue(AiFeedback.failure(new java.net.http.HttpTimeoutException("slow")).contains("timed out"));
        assertEquals("[hidden key]",AiFeedback.failure(new java.util.concurrent.ExecutionException(new IOException("sk-private_test_key"))));
        assertFalse(AiFeedback.failure(new IOException()).isBlank());
    }
    private AgentController agent(LlmClient client) throws IOException {
        var agent=new AgentController(root.resolve("data"),client);
        var semester=agent.createSemester("Fall "+UUID.randomUUID(),LocalDate.of(2026,9,1),LocalDate.of(2026,12,31));
        agent.createCourse(semester.id(),"EECS3311","#3366CC",Difficulty.MEDIUM);return agent;
    }
    @Test void networkAndMalformedModelAnswersDoNotCreateHistoryOrDeadlines() throws Exception {
        for(int mode=0;mode<3;mode++) {
            final int current=mode;
            var agent=agent((s,u)->{
                if(current==0)throw new java.net.http.HttpTimeoutException("timeout");
                if(current==1)throw new IOException("connection reset");
                return "not JSON";
            });
            Path source=root.resolve("lecture"+mode+".txt");Files.writeString(source,"Preconditions hold before a method call.");
            var file=agent.uploadMaterial(agent.courses().get(mode).id(),Category.LECTURE_MATERIALS,source);
            assertThrows(Exception.class,()->agent.answerFromMaterial(file.id(),"Explain contracts"));
            assertTrue(agent.history().stream().noneMatch(e->e.event().equals("question answered")));
            assertTrue(agent.schedule(AgentController.Period.ALL,null,"",LocalDate.of(2026,11,14)).isEmpty());assertTrue(agent.pendingProposals().isEmpty());
        }
    }
    @Test void missingSourceAndBlankQuestionsDoNotCallTheProvider() throws Exception {
        AtomicInteger calls=new AtomicInteger();var agent=agent((s,u)->{calls.incrementAndGet();return "{}";});
        assertThrows(IllegalArgumentException.class,()->agent.answerQuestion(agent.courses().get(0).id(),"explain contracts"));
        assertThrows(IllegalArgumentException.class,()->agent.answerQuestion(agent.courses().get(0).id()," "));
        assertEquals(0,calls.get());
    }
    @Test void incompleteImageReadDoesNotLeaveASuccessfulCacheAndCanBeRetried() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        LlmClient client=new LlmClient(){
            public String complete(String s,String u){return "{}";}
            public boolean supportsImages(){return true;}
            public String completeWithImages(String s,String u,List<String> images){
                return switch(calls.incrementAndGet()){
                    case 1 -> "{\"pages\":[]}";
                    case 2 -> "{\"pages\":[{\"page\":1,\"text\":\"\"}]}";
                    case 3 -> "{\"pages\":[{\"page\":1,\"text\":\"a\"},{\"page\":1,\"text\":\"b\"}]}";
                    default -> "{\"pages\":[{\"page\":1,\"text\":\"The caller establishes the precondition.\"}]}";
                };
            }
        };
        var agent=agent(client);Path pdf=root.resolve("scan.pdf");
        try(var doc=new org.apache.pdfbox.pdmodel.PDDocument()){
            doc.addPage(new org.apache.pdfbox.pdmodel.PDPage());doc.save(pdf.toFile());
        }
        var file=agent.uploadMaterial(agent.courses().get(0).id(),Category.LECTURE_MATERIALS,pdf);
        Path cache=root.resolve("data/artifacts/source-pages/"+file.id()+".json");
        for(int attempt=0;attempt<3;attempt++){
            assertThrows(IOException.class,()->agent.prepareMaterials(List.of(file.id()),m->{}));assertFalse(Files.exists(cache));
        }
        agent.prepareMaterials(List.of(file.id()),m->{});assertTrue(Files.exists(cache));assertEquals(4,calls.get());
    }
    @Test void cancellationDoesNotPersistPartialImageSources() throws Exception {
        var agent=agent(new LlmClient(){
            public String complete(String s,String u){return "{}";}
            public boolean supportsImages(){return true;}
        });
        Path pdf=root.resolve("cancel.pdf");
        try(var doc=new org.apache.pdfbox.pdmodel.PDDocument()){
            doc.addPage(new org.apache.pdfbox.pdmodel.PDPage());doc.save(pdf.toFile());
        }
        var file=agent.uploadMaterial(agent.courses().get(0).id(),Category.LECTURE_MATERIALS,pdf);
        Thread.currentThread().interrupt();
        try {assertThrows(IOException.class,()->agent.prepareMaterials(List.of(file.id()),m->{}));}
        finally {Thread.interrupted();}
        assertFalse(Files.exists(root.resolve("data/artifacts/source-pages/"+file.id()+".json")));
    }
    @Test void citationLinksRetainExactPagesAndTheirOriginalCourseCallback() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var text=new ReadableText();var clicks=new ArrayList<String>();
            var first=new GroundedAnswer.Citation("syllabus.pdf",7);
            text.setMarkdown("Study plan.");text.onCitation(c->clicks.add("course1:"+c.page()));text.appendCitations(List.of(first));
            int firstPosition=text.getText().indexOf("syllabus.pdf");
            var second=new GroundedAnswer.Citation("lecture.pdf",21);
            text.onCitation(c->clicks.add("course2:"+c.page()));text.appendCitations(List.of(second));
            assertEquals(first,text.citationAt(firstPosition));
            text.activateCitation(firstPosition);text.activateCitation(text.getText().indexOf("lecture.pdf"));
            assertEquals(List.of("course1:7","course2:21"),clicks);
            text.activateCitation(-1);text.activateCitation(0);assertEquals(2,clicks.size());
            assertTrue(javax.swing.text.StyleConstants.isUnderline(text.getStyledDocument().getCharacterElement(firstPosition).getAttributes()));
        });
    }
}
