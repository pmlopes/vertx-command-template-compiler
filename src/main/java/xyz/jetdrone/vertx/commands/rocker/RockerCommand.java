package xyz.jetdrone.vertx.commands.rocker;

import com.fizzed.rocker.compiler.*;
import com.fizzed.rocker.model.JavaVersion;
import com.fizzed.rocker.model.TemplateModel;
import com.fizzed.rocker.runtime.ParserException;
import com.fizzed.rocker.runtime.RockerRuntime;
import io.vertx.core.cli.CLIException;
import io.vertx.core.cli.annotations.*;
import io.vertx.core.spi.launcher.DefaultCommand;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Name("rocker")
@Summary("Compiles rocker templates.")
public class RockerCommand extends DefaultCommand {

  private String templateDirectory;
  private String outputDirectory;
  private boolean failOnError;
  private String suffixRegex;
  private String extendsClass;
  private String extendsModelClass;
  private boolean discardLogicWhitespace;
  private String targetCharset;
  private boolean optimize;

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

  @Option(longName = "failOnError", flag = true)
  @Description("Fail on error.")
  @DefaultValue("true")
  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }

  @Option(longName = "suffixRegex")
  @Description("File suffix regular expression to match template files (default: \".*\\\\.rocker\\\\.(raw|html)$\").")
  @DefaultValue(".*\\.rocker\\.(raw|html)$")
  public void setSuffixRegex(String suffixRegex) {
    this.suffixRegex = suffixRegex;
  }

  @Option(longName = "extendsClass")
  @Description("Template shall extend the given class.")
  public void setExtendsClass(String extendsClass) {
    this.extendsClass = extendsClass;
  }

  @Option(longName = "extendsModelClass")
  @Description("Model shall extend the given class.")
  public void setExtendsModelClass(String extendsModelClass) {
    this.extendsModelClass = extendsModelClass;
  }

  @Option(longName = "discardLogicWhitespace", flag = true)
  @Description("Discard logic white space (default: false).")
  @DefaultValue("false")
  public void setDiscardLogicWhitespace(boolean discardLogicWhitespace) {
    this.discardLogicWhitespace = discardLogicWhitespace;
  }

  @Option(longName = "targetCharset")
  @Description("Target Charset (default: UTF-8).")
  @DefaultValue("UTF-8")
  public void setTargetCharset(String targetCharset) {
    this.targetCharset = targetCharset;
  }

  @Option(longName = "optimize", flag = true)
  @Description("Optimize (default: false).")
  @DefaultValue("false")
  public void setOptimize(boolean optimize) {
    this.optimize = optimize;
  }

  @Override
  public void run() throws CLIException {

    final RockerConfiguration configuration = new RockerConfiguration();
    final TemplateParser parser = new TemplateParser(configuration);
    final JavaGenerator generator = new JavaGenerator(configuration);
    final List<File> templateFiles = new ArrayList<>();

    parser.getConfiguration().setTemplateDirectory(new File(getCwd(), templateDirectory));
    generator.getConfiguration().setOutputDirectory(new File(getCwd(), outputDirectory));
    generator.getConfiguration().setClassDirectory(new File(getCwd(), outputDirectory + File.separator + "classes"));

    parser.getConfiguration().getOptions().setJavaVersion(JavaVersion.v1_8);

    if (extendsClass != null) {
      parser.getConfiguration().getOptions().setExtendsClass(extendsClass);
    }

    if (extendsModelClass != null) {
      parser.getConfiguration().getOptions().setExtendsModelClass(extendsModelClass);
    }

    parser.getConfiguration().getOptions().setDiscardLogicWhitespace(discardLogicWhitespace);

    if (targetCharset != null) {
      parser.getConfiguration().getOptions().setTargetCharset(targetCharset);
    }

    parser.getConfiguration().getOptions().setOptimize(optimize);

    // start compiling

    if (!configuration.getTemplateDirectory().exists() || !configuration.getTemplateDirectory().isDirectory()) {
      fatal("Template directory does not exist: " + configuration.getTemplateDirectory());
    }

    // loop thru template directory and match templates
    Collection<File> allFiles = RockerUtil.listFileTree(configuration.getTemplateDirectory());
    for (File f : allFiles) {
      if (f.getName().matches(suffixRegex)) {
        templateFiles.add(f);
      }
    }

    warn("Parsing " + templateFiles.size() + " rocker template files");

    int errors = 0;
    int generated = 0;

    final List<File> generatedFiles = new ArrayList<>();

    for (File f : templateFiles) {
      TemplateModel model;

      try {
        // parse model
        model = parser.parse(f);
      } catch (IOException | ParserException e) {
        if (e instanceof ParserException) {
          ParserException pe = (ParserException) e;
          err("Parsing failed for " + f + ":[" + pe.getLineNumber() + "," + pe.getColumnNumber() + "] " + pe.getMessage());
        } else {
          err("Unable to parse template: " + e.getMessage());
        }
        errors++;
        continue;
      }

      try {
        File outputFile = generator.generate(model);
        generated++;

        warn("Generated java source: " + outputFile);
        generatedFiles.add(outputFile);
      } catch (GeneratorException | IOException e) {
        fatal("Generating java source failed for " + f + ": " + e.getMessage());
      }

    }

    warn("Generated " + generated + " rocker java source files");

    if (errors > 0 && failOnError) {
      fatal("Caught " + errors + " errors.");
    }

    if (!configuration.getOptions().getOptimize()) {
      // save configuration
      if (!configuration.getClassDirectory().exists()) {
        if (!configuration.getClassDirectory().mkdirs()) {
          warn("Failed to mkdir: " + configuration.getClassDirectory());
        }
      }

      // use resource name, but strip leading slash
      // place it into the classes directory (not the compile directory)
      try{
        File configFile = new File(configuration.getClassDirectory(), RockerRuntime.CONF_RESOURCE_NAME.substring(1));
        configuration.write(configFile);
        warn("Generated rocker configuration " + configFile);
      }catch(IOException iox){
        fatal(iox.getMessage());
      }
    } else {
      warn("Optimize flag on. Did not generate rocker configuration file");
    }

    // save the generated file list
    try (PrintWriter out = new PrintWriter(new File(getCwd(), outputDirectory + File.separator + "templates.lst"))) {
      for (File f : generatedFiles) {
        out.println(f.getPath());
      }
    } catch (IOException iox) {
      fatal(iox.getMessage());
    }
  }

  private static void warn(String message) {
    System.err.println("\u001B[1m\u001B[33m" + message + "\u001B[0m");
  }

  private static void err(String message) {
    System.err.println("\u001B[1m\u001B[31m" + message + "\u001B[0m");
  }

  private static void fatal(String message) {
    System.err.println("\u001B[1m\u001B[31m" + message + "\u001B[0m");
    System.exit(1);
  }
}
