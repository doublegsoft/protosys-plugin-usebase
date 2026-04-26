package org.doublegsoft.prototype.usebase;

import com.doublegsoft.jcommons.lang.HashObject;
import com.doublegsoft.jcommons.lang.StringHolder;
import com.doublegsoft.jcommons.metabean.ModelDefinition;
import com.doublegsoft.jcommons.metabean.ObjectDefinition;
import com.doublegsoft.jcommons.metamodel.ApplicationDefinition;
import com.doublegsoft.jcommons.metamodel.UsecaseDefinition;
import com.doublegsoft.jcommons.programming.NamingConvention;
import com.doublegsoft.jcommons.programming.c.CConventions;
import com.doublegsoft.jcommons.programming.go.GoConventions;
import com.doublegsoft.jcommons.programming.objc.ObjcConventions;
import com.doublegsoft.jcommons.programming.rust.RustConventions;
import com.doublegsoft.jcommons.utils.Inflector;
import com.doublegsoft.jcommons.utils.Strings;
import com.google.gson.Gson;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.doublegsoft.guidbase.GuidbaseMiniContext;
import io.doublegsoft.modelbase.Modelbase;
import io.doublegsoft.typebase.Typebase;
import io.doublegsoft.usebase.Usebase;

//import io.doublegsoft.usebase.aggregate.AggregateBuilder;
//import io.doublegsoft.usebase.association.AssociationBuilder;
import io.doublegsoft.usebase.modelbase.ModelbaseWriter;
import io.doublegsoft.usebase.projection.ProjectionBuilder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.doublegsoft.protosys.commons.FileSystemTemplateBasedPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Uses modelbase dsl language approach to develop software.
 *
 * <p>
 *   DDD (Domain Driven Design) example below:
 * <ul>
 *   <li>
 *     {@code
 *     domain =&lt; domain object, default sql, default command, default command handler, default event, default event
 *     handler
 *     }
 *   </li>
 *   <li>
 *     {@code
 *     command (source) => command object, command handler with event trigger (manually), specific sql
 *     }
 *   </li>
 *   <li>
 *     {@code
 *     event => event object, event handler (manually)
 *     }
 *   </li>
 *   <li>
 *     {@code
 *     query (sql) => query object, query handler (manually), common query ?
 *     }
 *   </li>
 *   <li>
 *     {@code
 *     sql => specific sql
 *     }
 *   </li>
 *   <li>
 *     {@code
 *     webapp (command, query) => web controller
 *     }
 *   </li>
 *   <li>
 *     {@code
 *     webapi (command, query) => payload object, api controller
 *     }
 *   </li>
 * </ul>
 *
 * @author <a href="mailto:guo.guo.gan@gmail.com">Christian Gann</a>
 *
 * @since 1.0
 */
public class UsebasePlugin extends FileSystemTemplateBasedPlugin {

  /**
   * Generates prototype source code for application definition.
   *
   * @param app
   *        the application definition
   *
   * @param model
   *        the model definition
   *
   * @param outputRoot
   *        the output root directory
   *
   * @param templateRoot
   *        the template root directory
   *
   * @param globals
   *        the global variables
   *
   * @throws IOException
   *        in case of any io errors
   *
   * @since 1.0
   *
   * @version 3.1 - added statics freemarker variable support on Mar 15, 2019 <br>
   */
  public void prototype(ApplicationDefinition app, ModelDefinition model, String outputRoot, String templateRoot, HashObject globals) throws IOException {
    FileTemplateLoader specific = new FileTemplateLoader(new File(templateRoot));
    // FileTemplateLoader specificForTest = new FileTemplateLoader(new File("/Volumes/EXPORT/local/works/doublegsoft.io/modelbase/03.Development/modelbase-data"));
    FileTemplateLoader common = new FileTemplateLoader(new File(templateRoot + "/.."));
    FileTemplateLoader common2 = new FileTemplateLoader(new File(templateRoot + "/../.."));
    MultiTemplateLoader templateLoader = new MultiTemplateLoader(new TemplateLoader[]{common, common2, specific/*, specificForTest*/});
    FREEMARKER.setTemplateLoader(templateLoader);
    FREEMARKER.setSharedVariable("statics", ((DefaultObjectWrapper) FREEMARKER.getObjectWrapper()).getStaticModels());

    decorate(model, globals);
    decorate(app, globals);
    if (globals != null) {
      globalVariables.putAll(globals);
    }
    app.setModel(model);
    visitAndRender(outputRoot, "", templateRoot, "", app, new HashObject());
  }

  public void prototype(ApplicationDefinition app, ModelDefinition dataModel, List<UsecaseDefinition> usecases, String outputRoot, String templateRoot, HashObject globals) throws IOException {
    FileTemplateLoader specific = new FileTemplateLoader(new File(templateRoot));
    // FileTemplateLoader specificForTest = new FileTemplateLoader(new File("/Volumes/EXPORT/local/works/doublegsoft.io/modelbase/03.Development/modelbase-data"));
    FileTemplateLoader common = new FileTemplateLoader(new File(templateRoot + "/.."));
    FileTemplateLoader common2 = new FileTemplateLoader(new File(templateRoot + "/../.."));
    MultiTemplateLoader templateLoader = new MultiTemplateLoader(new TemplateLoader[]{common, common2, specific/*, specificForTest*/});
    FREEMARKER.setTemplateLoader(templateLoader);
    FREEMARKER.setSharedVariable("statics", ((DefaultObjectWrapper) FREEMARKER.getObjectWrapper()).getStaticModels());

    app.setModel(dataModel);
    globals.set("usecases", usecases);
//    globals.set("aggregateBuilder", new AggregateBuilder(dataModel));
//    globals.set("associationBuilder", new AssociationBuilder(dataModel));

    NamingConvention nc = globals.get("globalNamingConvention");
    List<String> templatePaths = getTemplateRelativePaths(templateRoot);
    for (String templatePath : templatePaths) {
      String realRelativePath = templatePath;
      if (realRelativePath.contains("$namespace$")) {
        String namespacePath = globals.get("namespace");
        namespacePath = namespacePath.replaceAll("\\.", "/");
        realRelativePath = realRelativePath.replace("$namespace$", namespacePath);
      }
      if (realRelativePath.contains("$app$")) {
        realRelativePath = realRelativePath.replace("$app$", app.getName());
      }
      if (realRelativePath.contains("$usecase$")) {
        for (UsecaseDefinition usecase : usecases) {
          globals.set("usecase", usecase);
          String usecaseRelativePath = realRelativePath;
          usecaseRelativePath = usecaseRelativePath.replace("$usecase$", nc.nameFile(usecase.getName()));
          usecaseRelativePath = usecaseRelativePath.replaceAll("\\.ftl", "");
          outputSourceCode(templatePath, outputRoot, usecaseRelativePath, globals);
        }
      }
    }
  }

  public static void outputSourceCode(String templatePath, String outputRoot,
                                      String relativePath, HashObject globals) throws IOException {
    Template tpl = FREEMARKER.getTemplate(templatePath);
    Map<String, Object> data = new HashMap<>();
    data.putAll(globals);
    File file = new File(outputRoot + "/" + relativePath);
    file.getParentFile().mkdir();
    FileWriter fw = new FileWriter(file);
    try {
      tpl.process(data, fw);
    } catch (TemplateException ex) {
      throw new IOException(ex);
    }
    fw.close();
  }

  public static List<String> getTemplateRelativePaths(String templateRoot) throws IOException {
    Path root = Paths.get(templateRoot).toAbsolutePath().normalize();

    try (Stream<Path> paths = Files.walk(root)) {
      return paths
          .filter(Files::isRegularFile)
          .map(path -> root.relativize(path).toString().replace("\\", "/"))
          .collect(Collectors.toList());
    }
  }

  public static void main(String[] args) throws Exception {
    Options options = new Options();

    options.addOption("u", "usebase", true, "Usebase模型定义文件");
    options.addOption("m", "modelbase", true, "Modelbase模型定义文件");
    options.addOption("t", "template-root", true, "模板定义根目录");
    options.addOption("o", "output-root", true, "输出跟路径");
    options.addOption("b", "tatabase", true, "Tatabase数据目录");
    options.addOption("l", "license", true, "license数据文件");
    options.addOption("g", "globals", true, "全局常量");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    String usebasePath = cmd.getOptionValue("usebase");
    String modelbasePath = cmd.getOptionValue("modelbase");
    String templateRoot = cmd.getOptionValue("template-root");
    String outputRoot = cmd.getOptionValue("output-root");
    String tatabase = cmd.getOptionValue("tatabase");
    String licensePath = cmd.getOptionValue("license");
    String globals = cmd.getOptionValue("globals");

    // globals
    HashObject globalVars = new HashObject();
    Gson gson = new Gson();
    if (globals != null) {
      globalVars.putAll(gson.fromJson(globals, Map.class));
    }

    // license
    String license = null;
    if (licensePath != null) {
      license = new String(Files.readAllBytes(new File(licensePath).toPath()), "UTF-8");
      globalVars.set("license", license);
    }

    globalVars.set("typebase", new Typebase());
//    globalVars.set("tatabase", new Tatabase());
    globalVars.set("guidbase_mini", new GuidbaseMiniContext());
    globalVars.set("c", new CConventions());
    globalVars.set("rust", new RustConventions());
    globalVars.set("go", new GoConventions());
    globalVars.set("objc", new ObjcConventions());
    globalVars.set("inflector", new Inflector());

    UsebasePlugin usebasePlugin = new UsebasePlugin();

    Modelbase modelbase = new Modelbase();
    ModelDefinition dataModel = usebasePlugin.createModelFromModelbase(modelbasePath.split(";"));

    List<UsecaseDefinition> usecases = createUsecasesFromUsebase(dataModel, usebasePath.split(";"));
    ProjectionBuilder projBuilder = new ProjectionBuilder(dataModel);

    List<ObjectDefinition> rowObjs = new ArrayList<>();
    for (ObjectDefinition obj : dataModel.getObjects()) {
      ObjectDefinition rowObj = projBuilder.build(obj);
      rowObjs.add(rowObj);
    }
    StringWriter sw = new StringWriter();
    ModelbaseWriter writer = new ModelbaseWriter(sw, dataModel);
    for (UsecaseDefinition usecase : usecases) {
      if (usecase != null) {
        writer.write(usecase.getParameterizedObject());
        if (usecase.getReturnedObject() != null) {
          writer.write(usecase.getReturnedObject());
        }
      }
      for (ObjectDefinition obj : rowObjs) {
        writer.write(obj);
      }
    }
    ModelDefinition usebaseDataModel = modelbase.parse(sw.toString());

    globalVars.set("usecases", usecases);

    for (ObjectDefinition obj : dataModel.getObjects()) {
      /*!
       ** Modlebase对Module模块分类的支持。
       **
       ** 2025-01-04
       */
      if (obj.isLabelled("module")) {
        obj.setModuleName(obj.getLabelledOptions("module").get("name"));
      }
    }

    ApplicationDefinition app = new ApplicationDefinition();

    String applicationName = globalVars.get("application");
    String databaseName = globalVars.get("database");
    if (applicationName == null) {
      applicationName = globalVars.get("application");
    }
    app.setName(applicationName);
    app.setModel(dataModel);

    String namingClass = globalVars.get("naming");
    if (namingClass != null) {
      Object naming = Class.forName(namingClass).newInstance();
      globalVars.set("naming", naming);
    }

    namingClass = globalVars.get("globalNamingConvention");
    if (namingClass != null) {
      Object naming = Class.forName(namingClass).newInstance();
      globalVars.set("globalNamingConvention", naming);
    }

    for (ObjectDefinition obj : dataModel.getObjects()) {
      if (obj.isLabelled("generated")) {
        // ignore dependant modules
        continue;
      }
      if (!Strings.isEmpty(databaseName)) {
        if (obj.isLabelled("persistence")) {
          obj.getLabelledOptions("persistence").put("namespace", databaseName);
        }
      }
      Map<String, String> moduleOpts = new HashMap<>();
      moduleOpts.putAll(obj.getLabelledOptions("module"));
      if (obj.getModuleName() == null) {
        obj.setModuleName(applicationName);
        moduleOpts.put("name", applicationName);
      }
      obj.setLabelledOptions("module", moduleOpts);
    }

    try {
      // modelbase from usebase
      usebasePlugin.prototype(app, usebaseDataModel, outputRoot, templateRoot, globalVars);
      usebasePlugin.prototype(app, dataModel, usecases, outputRoot, templateRoot, globalVars);
    } catch (Throwable cause) {
      cause.printStackTrace();
      System.out.println(cause.getMessage());
    }

  }

  private static List<UsecaseDefinition> createUsecasesFromUsebase(ModelDefinition dataModel, String... usebasePaths) throws IOException {
    Usebase usebase = new Usebase(dataModel);
    StringHolder dsl = new StringHolder();
    for (String path : usebasePaths) {
      if (Strings.isEmpty(path.trim())) {
        continue;
      }
      File file = new File(path);
      if (file.exists() && !file.isDirectory()) {
        dsl.append(new String(Files.readAllBytes(file.toPath()), "UTF-8")).linefeed();
      }
    }
    return usebase.parse(dsl.toString());
  }

}

