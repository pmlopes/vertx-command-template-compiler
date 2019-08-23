package xyz.jetdrone.vertx.commands.rocker;

import org.junit.Test;

import static org.junit.Assert.*;

public class RockerCommandTest {

  final RockerCommand cmd = new RockerCommand();

  @Test
  public void compileTemplate() {
    cmd.setTemplateDirectory("src/test/resources");
    cmd.setOutputDirectory("target");
    cmd.setSuffixRegex(".*\\.rocker\\.(raw|html)$");
    cmd.run();
  }
}
