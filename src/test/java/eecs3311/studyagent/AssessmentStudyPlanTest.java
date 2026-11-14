package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AssessmentStudyPlanTest {
    @TempDir Path root;
    private AgentController make(LlmClient client) throws Exception {
        var agent=new AgentController(root.resolve("data"),client);
        var semester=agent.createSemester("Fall",LocalDate.of(2026,9,1),LocalDate.of(2026,12,31));
        agent.createCourse(semester.id(),"EECS3311","#3366CC",Difficulty.MEDIUM);return agent;
    }
    private void upload(AgentController agent,String name,String text,Category category) throws Exception {
        Path file=root.resolve(name);Files.writeString(file,text);agent.uploadMaterial(agent.courses().get(0).id(),category,file);
    }
    private String answer(boolean lectures) {
        return "{\"answer\":\"Trace withdraw and write requires/ensures from the slides.\",\"citations\":[{\"fileName\":\"syllabus.txt\",\"page\":1}"
                +(lectures?",{\"fileName\":\"dbc.txt\",\"page\":1},{\"fileName\":\"jml.txt\",\"page\":1}":"")+"]}";
    }
    @Test void preparationReadsEverySelectedLectureIncludingTailsInsteadOfOnlySyllabusDates() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        var agent=make((s,u)->{
            calls.incrementAndGet();
            if(s.startsWith("Select uploaded lecture sources")) {
                assertTrue(u.contains("Lab Test 2: Design by Contract"));assertTrue(u.contains("unrelated.txt"));
                return "{\"scope\":\"Design by Contract; detailed questions unannounced\",\"fileNames\":[\"dbc.txt\",\"jml.txt\"]}";
            }
            assertTrue(u.contains("DBC_LAST_WORKED_EXAMPLE"));assertTrue(u.contains("JML_LAST_REQUIRES_ENSURES"));
            assertFalse(u.contains("UNRELATED_ARCHITECTURE_BODY"));
            assertTrue(u.contains("common mistakes"));assertTrue(u.contains("3 self-check questions"));
            return answer(true);
        });
        upload(agent,"syllabus.txt","Lab Test 2: Design by Contract, November 18",Category.SYLLABUS);
        upload(agent,"dbc.txt","Contracts ".repeat(3500)+"DBC_LAST_WORKED_EXAMPLE",Category.LECTURE_MATERIALS);
        upload(agent,"jml.txt","Java specifications ".repeat(1800)+"JML_LAST_REQUIRES_ENSURES",Category.LECTURE_MATERIALS);
        upload(agent,"unrelated.txt","UNRELATED_ARCHITECTURE_BODY",Category.LECTURE_MATERIALS);
        var result=agent.answerQuestion(agent.courses().get(0).id(),"How do I prepare for labtest 2?");
        assertEquals(2,calls.get());assertEquals(3,result.citations().size());
    }
    @Test void syllabusOnlyAnswerIsRegeneratedWithTeachingEvidence() throws Exception {
        AtomicInteger qaCalls=new AtomicInteger();
        var agent=make((s,u)->{
            if(s.startsWith("Select uploaded lecture sources"))return "{\"scope\":\"Contracts\",\"fileNames\":[\"dbc.txt\",\"jml.txt\"]}";
            return answer(qaCalls.incrementAndGet()>1);
        });
        upload(agent,"syllabus.txt","Lab Test 2 Contracts",Category.SYLLABUS);
        upload(agent,"dbc.txt","Trace a withdraw precondition and postcondition",Category.LECTURE_MATERIALS);
        upload(agent,"jml.txt","Write requires and ensures clauses",Category.LECTURE_MATERIALS);
        var result=agent.answerQuestion(agent.courses().get(0).id(),"How should I study for Lab Test 2?");
        assertEquals(2,qaCalls.get());assertEquals(3,result.citations().size());
        assertEquals(1,agent.history().stream().filter(e->e.event().equals("question answered")).count());
    }
    @Test void unavailableLectureSelectionFailsWithoutSavingAPlan() throws Exception {
        var agent=make((s,u)->"{\"scope\":\"Contracts\",\"fileNames\":[\"another-course.pdf\"]}");
        upload(agent,"dbc.txt","Contracts",Category.LECTURE_MATERIALS);
        assertThrows(IOException.class,()->agent.answerQuestion(agent.courses().get(0).id(),"How to prepare for final?"));
        assertTrue(agent.history().stream().noneMatch(e->e.event().equals("question answered")));
    }
    @Test void largeStudyScopeReadsAllBatchesAndValidatesPhysicalPages() throws Exception {
        StringBuilder read=new StringBuilder();
        var agent=make((s,u)->{
            if(s.startsWith("Select uploaded lecture sources"))return "{\"scope\":\"General preparation; final scope unknown\",\"fileNames\":[\"large.txt\"]}";
            if(s.startsWith("Read the entire source batch")){
                read.append(u);return "{\"notes\":[{\"page\":1,\"text\":\"Practise the source contract example.\"}]}";
            }
            assertTrue(u.length()<100000,"Final answer uses grounded notes after every source batch was read");
            return "{\"answer\":\"Practise the source example.\",\"citations\":[{\"fileName\":\"large.txt\",\"page\":1}]}";
        });
        upload(agent,"large.txt","START_SOURCE "+"Contract example ".repeat(7500)+" LAST_SOURCE",Category.LECTURE_MATERIALS);
        agent.answerQuestion(agent.courses().get(0).id(),"How do I study for final?");
        assertTrue(read.toString().contains("START_SOURCE"));assertTrue(read.toString().contains("LAST_SOURCE"));
        assertTrue(read.toString().split("Return ",-1).length>3);
    }

    @Test void quizzesUseTeachingFilesAndNeverSyllabusLogistics() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        var agent=make((s,u)->{
            calls.incrementAndGet();assertTrue(u.contains("TEACHING_WITHDRAW_EXAMPLE"));assertFalse(u.contains("SYLLABUS_LOGISTICS_ONLY"));
            return "{\"questions\":[{\"topic\":\"Contracts\",\"question\":\"When does the caller check a precondition?\",\"choices\":[\"Before\",\"After\",\"Never\",\"Only at shutdown\"],\"answerIndex\":0,\"explanation\":\"The source says before.\"}]}";
        });
        upload(agent,"syllabus.txt","SYLLABUS_LOGISTICS_ONLY",Category.SYLLABUS);
        upload(agent,"dbc.txt","TEACHING_WITHDRAW_EXAMPLE",Category.LECTURE_MATERIALS);
        var syllabus=agent.materials().stream().filter(f->f.category()==Category.SYLLABUS).findFirst().orElseThrow();
        var lecture=agent.materials().stream().filter(f->f.category()==Category.LECTURE_MATERIALS).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class,()->agent.generateQuiz(syllabus.id(),1,Difficulty.MEDIUM));
        assertThrows(IllegalArgumentException.class,()->agent.generateSlideQuiz(syllabus.id(),List.of(1),1,Difficulty.MEDIUM));
        assertEquals(0,calls.get());
        var quiz=agent.generateQuiz(List.of(syllabus.id(),lecture.id()),1,Difficulty.MEDIUM);
        assertEquals(lecture.id(),quiz.materialId());assertEquals(1,calls.get());
    }
}
