package eecs3311.studyagent;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuntimeDiagnosticsTest {
    @TempDir Path root;
    @Test void recordsTheActualLinkageFailureAndImageWithoutLeakingAnApiKey() throws Exception {
        String secret=String.join("-", "sk", "proj", "ExampleSecretForRedaction123456789");
        var error=new NoClassDefFoundError("StudyArtifacts$EssayGrade " + secret);
        Path log=RuntimeDiagnostics.record(root,"AWT-EventQueue-0",error);
        String text=Files.readString(log);
        assertTrue(text.contains("NoClassDefFoundError"));
        assertTrue(text.contains("StudyArtifacts$EssayGrade"));
        assertTrue(text.contains("Loaded image:"));
        assertFalse(text.contains(secret));
        assertTrue(text.contains("[hidden key]"));
    }
}
