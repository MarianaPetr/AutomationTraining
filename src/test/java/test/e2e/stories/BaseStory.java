package test.e2e.stories;

import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.embedder.EmbedderControls;
import org.jbehave.core.embedder.StoryControls;
import org.jbehave.core.failures.FailingUponPendingStep;
import org.jbehave.core.failures.PendingStepStrategy;
import org.jbehave.core.io.CodeLocations;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.io.StoryFinder;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.reporters.CrossReference;
import org.jbehave.core.reporters.Format;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.jbehave.web.selenium.SeleniumConfiguration;
import org.jbehave.web.selenium.SeleniumContext;
import org.jbehave.web.selenium.SeleniumContextOutput;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import test.e2e.steps.AuthenticationSteps;
import test.e2e.steps.DishesSelectionSteps;
import test.e2e.steps.OrderSteps;
import utils.DataLoader;
import utils.StoryPathConverter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.jbehave.core.reporters.Format.CONSOLE;
import static org.jbehave.web.selenium.WebDriverHtmlOutput.WEB_DRIVER_HTML;

public class BaseStory extends JUnitStories {
    private static final String SELENIUM_VERSION = DataLoader.getWebDriverVersion();
    private static final File CurrentPath = new File("");
    private static final File PROJECT_PATH = new File(CurrentPath.getAbsolutePath());
    private static final String USERNAME = System.getenv("SAUCE_USERNAME");
    private static final String ACCESS_KEY = System.getenv("SAUCE_ACCESS_KEY");
    private static final String URL = "https://" + USERNAME + ":" + ACCESS_KEY + "@ondemand.saucelabs.com:443/wd/hub";
    private static WebDriver browser;
    private static String CHROME_DRIVER_PATH = "/utils/" + SELENIUM_VERSION + "/chromedriver";

    static {
        if (System.getProperty("os.name").startsWith("Windows")) {
            CHROME_DRIVER_PATH += ".exe";
        }
    }

    private PendingStepStrategy pendingStepStrategy = new FailingUponPendingStep();
    private CrossReference crossReference = new CrossReference().withJsonOnly().withOutputAfterEachStory(true);
    private SeleniumContext seleniumContext = new SeleniumContext();

    private Format[] formats = new Format[]{
            new SeleniumContextOutput(seleniumContext), CONSOLE, WEB_DRIVER_HTML
    };

    private StoryReporterBuilder reporterBuilder = new StoryReporterBuilder()
            .withCodeLocation(CodeLocations.codeLocationFromClass(BaseStory.class))
            .withFailureTrace(true)
            .withFailureTraceCompression(true)
            .withDefaultFormats()
            .withFormats(formats)
            .withCrossReference(crossReference);

    public BaseStory() {
        EmbedderControls embedderControls = configuredEmbedder().embedderControls();
        embedderControls.doIgnoreFailureInView(true);
        configuredEmbedder().embedderControls();
    }

    @BeforeClass
    public static void createAndStartService() {
        startSauceLabsWebDriver();
    }

    private static void startSauceLabsWebDriver() {
        DesiredCapabilities caps = DesiredCapabilities.chrome();
        caps.setCapability("platform", "Windows XP");
        caps.setCapability("version", "43.0");

        try {
            browser = new RemoteWebDriver(new URL(URL), caps);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }

        browser.manage().window().maximize();
    }

    private static void startLocalChromeDriver() {
        ChromeDriverService service;
        try {
            service = new ChromeDriverService.Builder()
                    .usingDriverExecutable(new File(PROJECT_PATH + CHROME_DRIVER_PATH))
                    .usingAnyFreePort()
                    .build();
            service.start();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        browser = new RemoteWebDriver(service.getUrl(), DesiredCapabilities.chrome());
    }

    public static WebDriver getBrowser() {
        return browser;
    }

    @Before
    public final void clean() throws IOException {
        try {
            File dir = new File("target/jbehave");
            for (File file : dir.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        } catch (Exception ex) {
        }
    }

    @After
    public final void afterStory() throws Exception {
        if (browser != null) {
            browser.quit();
        }
    }

    @Override
    public final List<String> storyPaths() {
        //URL codeLocation = CodeLocations.codeLocationFromPath("src/test/resources/");
        String codeLocation = "src/test/resources/";
        StoryPathConverter.convertStringToListOfStoryPathes(DataLoader.storiesToRun());
        return new StoryFinder()
                .findPaths(codeLocation, StoryPathConverter.convertStringToListOfStoryPathes(DataLoader.storiesToRun()), null);
    }

    @Override
    public final Configuration configuration() {
        return new SeleniumConfiguration().useSeleniumContext(seleniumContext)
                .usePendingStepStrategy(pendingStepStrategy)
                .useStoryControls(new StoryControls().doResetStateBeforeScenario(false))
                .useStoryLoader(new LoadFromClasspath(BaseStory.class))
                .useStoryReporterBuilder(reporterBuilder);
    }

    @Override
    public InjectableStepsFactory stepsFactory() {
        return new InstanceStepsFactory(configuration(),
                new AuthenticationSteps(),
                new DishesSelectionSteps(),
                new OrderSteps()
        );
    }
}

