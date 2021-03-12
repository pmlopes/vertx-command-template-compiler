package xyz.jetdrone.vertx.commands.jte;

import io.vertx.core.spi.launcher.DefaultCommandFactory;

public class JteCommandFactory extends DefaultCommandFactory<JteCommand> {

  public JteCommandFactory() {
    super(JteCommand.class, JteCommand::new);
  }
}
