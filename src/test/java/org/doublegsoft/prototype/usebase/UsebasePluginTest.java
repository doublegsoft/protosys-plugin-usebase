package org.doublegsoft.prototype.usebase;

import io.doublegsoft.usebase.Usebase;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class UsebasePluginTest {

  @Test
  public void test_main() throws Exception {
    String modelbaseContent = new String(Files.readAllBytes(
        new File("src/test/resources/modelbase/wfm.modelbase").toPath()), "UTF-8");
    String usebaseContent = new String(Files.readAllBytes(
        new File("src/test/resources/usebase/wfm.usebase").toPath()), "UTF-8");

    List<String> args = new ArrayList<>();
    args.add("--model=src/test/resources/modelbase/wfm.modelbase");
    args.add("--usebase=src/test/resources/usebase/wfm.usebase");
    args.add("--template-root=src/test/resources/template");
    args.add("--output-root=target/gen-out");
    args.add("--license=src/test/resources/LICENSE");
    args.add("--globals={\"namespace\":\"hello.world\"," +
        "\"application\":\"test\"," +
        "\"naming\":\"com.doublegsoft.jcommons.programming.java.JavaConventions\"," +
        "\"globalNamingConvention\":\"com.doublegsoft.jcommons.programming.java.JavaNamingConvention\","+
        "\"language\":\"java\"}");
    UsebasePlugin.main(args.toArray(new String[0]));
  }

}
