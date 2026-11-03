package eecs3311.studyagent;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() { }

    public static void main(String[] args) throws Exception {
        boolean cli = Arrays.asList(args).contains("--cli");
        Path projectRoot = Path.of(System.getProperty("project2.root", ".")).toAbsolutePath().normalize();
        if (System.getProperty("project2.root") == null
                && projectRoot.getFileName() != null
                && projectRoot.getFileName().toString().equalsIgnoreCase("project2")
                && Files.isDirectory(projectRoot.resolveSibling("project1")))
            projectRoot = projectRoot.getParent();
        Path data = projectRoot.resolve("data");
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--data")) {
                if (i + 1 >= args.length) throw new IllegalArgumentException("--data needs a directory path.");
                data = Path.of(args[++i]);
            } else if (!args[i].equals("--cli")) {
                throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }
        AgentController controller = new AgentController(data, new HttpLlmClient(projectRoot));
        if (cli) {
            new CommandLineInterface(controller).run();
        } else {
            if (GraphicsEnvironment.isHeadless()) {
                throw new IllegalStateException("No desktop display is available. Run with --cli.");
            }
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { /* Keep the standard Swing theme when unavailable. */ }
            SwingUtilities.invokeLater(() -> new MainWindow(controller).setVisible(true));
        }
    }
}
