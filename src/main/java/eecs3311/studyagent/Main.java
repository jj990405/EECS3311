package eecs3311.studyagent;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import com.formdev.flatlaf.FlatDarkLaf;

public final class Main {
    private Main() { }

    public static void main(String[] args) throws Exception {
        boolean cli = Arrays.asList(args).contains("--cli");
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace();
            if (!cli && error instanceof LinkageError) SwingUtilities.invokeLater(() ->
                    javax.swing.JOptionPane.showMessageDialog(null,
                            "Application classes could not be loaded. Close Study Agent and reopen it. "
                            + "Do not replace app files while it is running.", "Restart Study Agent",
                            javax.swing.JOptionPane.ERROR_MESSAGE));
        });
        Path projectRoot = Path.of(System.getProperty("studyagent.root", "."))
                .toAbsolutePath().normalize();
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
            FlatDarkLaf.setup();
            AppTheme.install();
            SwingUtilities.invokeLater(() -> new MainWindow(controller).setVisible(true));
        }
    }
}
