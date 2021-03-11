package xyz.jetdrone.vertx.commands.jte;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.spi.launcher.DefaultCommand;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Name("jte")
@Summary("Compiles jte templates.")
public class JteCommand extends DefaultCommand {

  private String templateDirectory;
  private String outputDirectory;

  private String contentType = "Html";
  public boolean trimControlStructures;
  public String[] htmlTags;
  public String[] htmlAttributes;
  public boolean htmlCommentsPreserved;

  @Argument(index = 0, argName = "templateDirectory")
  @Description("Directory containing templates.")
  public void setTemplateDirectory(String templateDirectory) {
    this.templateDirectory = templateDirectory;
  }

  @Option(longName = "outputDirectory", shortName = "d")
  @Description("Directory to output generated Java source files.")
  @DefaultValue("out")
  public void setOutputDirectory(String outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  @Option(longName = "contentType")
  @Description("Content Type for the template (default: Html).")
  @DefaultValue("Html")
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  @Option(longName = "trimControlStructures", shortName = "ws", flag = true)
  @Description("Trim Control Structures (default: false).")
  public void setTrimControlStructures(boolean trimControlStructures) {
    this.trimControlStructures = trimControlStructures;
  }

  @Option(longName = "htmlTags")
  @Description("HTML Tags.")
  public void setHtmlTags(String htmlTags) {
    this.htmlTags = htmlTags.split(",");
  }

  @Option(longName = "htmlAttributes")
  @Description("HTML Attributes.")
  public void setHtmlAttributes(String htmlAttributes) {
    this.htmlAttributes = htmlAttributes.split(",");
  }

  @Option(longName = "htmlCommentsPreserved", shortName = "D", flag = true)
  @Description("HTML Comments Preserved (default: false).")
  public void setHtmlCommentsPreserved(boolean htmlCommentsPreserved) {
    this.htmlCommentsPreserved = htmlCommentsPreserved;
  }

  @Override
  public void run() throws CLIException {

    long start = System.nanoTime();

    Path source = Paths.get(templateDirectory);
    Path target = Paths.get(outputDirectory);

    warn("Generating jte templates found in " + source);

    TemplateEngine templateEngine = TemplateEngine.create(new DirectoryCodeResolver(source), target, ContentType.valueOf(contentType));
    templateEngine.setTrimControlStructures(trimControlStructures);
    templateEngine.setHtmlTags(htmlTags);
    templateEngine.setHtmlAttributes(htmlAttributes);
    templateEngine.setHtmlCommentsPreserved(htmlCommentsPreserved);

    List<String> generated;

    try {
      templateEngine.cleanAll();
      generated = templateEngine.generateAll();
    } catch (Exception e) {
      fatal("Failed to generate templates: " + e.getMessage());
      return;
    }

    long end = System.nanoTime();
    long duration = TimeUnit.NANOSECONDS.toSeconds(end - start);
    int amount = generated.size();
    warn("Successfully generated " + amount + " jte file" + (amount == 1 ? "" : "s") + " in " + duration + "s to " + target);

    // save the generated file list
    try (PrintWriter out = new PrintWriter(new File(getCwd(), outputDirectory + File.separator + "templates.lst"))) {
      for (String file : generated) {
        out.println(file);
      }
    } catch (IOException iox) {
      fatal(iox.getMessage());
    }
  }

  private static void warn(String message) {
    System.err.println("\u001B[1m\u001B[33m" + message + "\u001B[0m");
  }

  private static void fatal(String message) {
    System.err.println("\u001B[1m\u001B[31m" + message + "\u001B[0m");
    System.exit(1);
  }
}
