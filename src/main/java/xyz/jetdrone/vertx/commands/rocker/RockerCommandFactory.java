package xyz.jetdrone.vertx.commands.rocker;

import io.vertx.core.spi.launcher.DefaultCommandFactory;

public class RockerCommandFactory extends DefaultCommandFactory<RockerCommand> {

  public RockerCommandFactory() {
    super(RockerCommand.class, RockerCommand::new);
  }
}
