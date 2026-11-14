package eecs3311.studyagent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Records the real error and loaded image, without configuration contents. */
final class RuntimeDiagnostics {
    private RuntimeDiagnostics() { }

    static void install(Path root, boolean cli) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace();
            Path saved = null;
            try { saved = record(root, thread.getName(), error); }
            catch (Exception loggingFailure) { System.err.println("Could not save the application diagnostic."); }
            if (!cli && error instanceof LinkageError) {
                String detail = error.getClass().getSimpleName();
                String message = "An application component could not be loaded (" + detail + ").\n"
                        + "Close Study Agent and reopen it to load the current version."
                        + (saved == null ? "" : "\nDetails were saved in codex/runtime-diagnostics/last-error.log.");
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, message,
                        "Restart Study Agent", JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    static Path record(Path root, String thread, Throwable error) throws Exception {
        StringWriter stack = new StringWriter();
        error.printStackTrace(new PrintWriter(stack));
        String image = RuntimeDiagnostics.class.getProtectionDomain().getCodeSource().getLocation().toString();
        String text = Instant.now() + "\nThread: " + thread + "\nLoaded image: " + image + "\n" + stack;
        text = text.replaceAll("sk-[A-Za-z0-9_-]+", "[hidden key]");
        Path folder = root.resolve("codex/runtime-diagnostics");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("last-error.log"), text);
        Files.writeString(folder.resolve("errors.log"), text + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return folder.resolve("last-error.log");
    }
}
