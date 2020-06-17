package it.fpagano.mvp.wiremock;

import com.github.jknack.handlebars.internal.lang3.tuple.ImmutablePair;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.recording.RecordSpec;
import com.github.tomakehurst.wiremock.stubbing.StubImport;
import com.github.tomakehurst.wiremock.stubbing.StubImportBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.recordSpec;

public class Main {

  public static void main(String[] args) throws IOException, InterruptedException {

    WireMockServer server = new WireMockServer(options());
    server.start();

    /*server.startRecording(config());

    StupidClient httpClient = new StupidClient();
    httpClient.joke();
    httpClient.hello();

    SnapshotRecordResult snapshotRecordResult = server.stopRecording();
    System.out.println("print");
    snapshotRecordResult.getStubMappings().forEach(System.out::println);
    System.out.println("stop");*/

    WireMockServer standalone = new WireMockServer(9191);
    standalone.start();

    List<StubMapping> stubMappings = server.getStubMappings();

    standalone.importStubs(stubs(stubMappings));

    stubMappings.stream()
            .map(stub -> stub.getResponse().getBodyFileName())
            .peek(s -> System.out.println("s = " + s))
            .map(path -> new ImmutablePair<>(path, fileContent(path)))
            .forEach(pair -> put(pair.left, pair.right));

    server.stop();
  }

  private static List<String> fileContent(String path) {
    try {
      return Files.readAllLines(Paths.get(path));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Collections.emptyList();
  }


  private static  void put(String fileName, List<String> lines) {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:9191/__admin/files/" + fileName))
            .method("PUT", HttpRequest.BodyPublishers.ofFile())
            .build();

    HttpResponse<String> response =
            null;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    System.out.println(response.body());
  }

  private static StubImport stubs(List<StubMapping> stubMappings) {
    StubImportBuilder stubImportBuilder = StubImport.stubImport();

    stubMappings.forEach(stubImportBuilder::stub);

    return stubImportBuilder.build();
  }

  private static Options options() throws IOException {

    String userHome = System.getProperty("user.home");
    Path ub5Files = Path.of(userHome, "UB5", "__files").toAbsolutePath();
    Path ub5Mappings = Path.of(userHome, "UB5", "mappings").toAbsolutePath();

    Files.createDirectories(ub5Files);
    Files.createDirectories(ub5Mappings);

    return WireMockConfiguration.options()
            .usingFilesUnderDirectory(Path.of(userHome, "UB5").toAbsolutePath().toString())
            .port(9090);

  }

  private static RecordSpec config() {
    return recordSpec()
            .forTarget(recordingServerUrl())
            .makeStubsPersistent(true)
            .ignoreRepeatRequests()
            .extractTextBodiesOver(0)
            .build();
  }

  private static String recordingServerUrl() {
    return "http://localhost:8080/";
  }
}
