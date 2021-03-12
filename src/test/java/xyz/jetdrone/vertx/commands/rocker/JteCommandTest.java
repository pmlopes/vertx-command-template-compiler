package xyz.jetdrone.vertx.commands.rocker;

import org.junit.Test;
import xyz.jetdrone.vertx.commands.jte.JteCommand;

public class JteCommandTest {

  final JteCommand cmd = new JteCommand();

  @Test
  public void compileTemplate() {
    cmd.setTemplateDirectory("src/test/resources");
    cmd.setOutputDirectory("target");
    cmd.run();
  }
}
