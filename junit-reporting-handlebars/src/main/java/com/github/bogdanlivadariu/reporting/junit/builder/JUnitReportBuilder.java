package com.github.bogdanlivadariu.reporting.junit.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;

import com.github.bogdanlivadariu.reporting.junit.helpers.Constants;
import com.github.bogdanlivadariu.reporting.junit.helpers.Helpers;
import com.github.bogdanlivadariu.reporting.junit.xml.models.TestCaseModel;
import com.github.bogdanlivadariu.reporting.junit.xml.models.TestSuiteModel;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

public class JUnitReportBuilder {
    private String TEST_SUMMARY_REPORT = "junit-reporting/testCaseSummaryReport";

    private String TEST_OVERVIEW_REPORT = "junit-reporting/testOverviewReport";

    private final String TEST_OVERVIEW_PATH;

    private final String TEST_SUMMARY_PATH;

    private List<TestSuiteModel> processedTestSuites;

    public JUnitReportBuilder(List<String> xmlReports, String targetBuildPath) throws FileNotFoundException,
        JAXBException {
        TEST_OVERVIEW_PATH = targetBuildPath + "/";
        TEST_SUMMARY_PATH = targetBuildPath + "/test-summary/";
        processedTestSuites = new ArrayList<>();

        JAXBContext cntx = JAXBContext.newInstance(TestSuiteModel.class);

        Unmarshaller unm = cntx.createUnmarshaller();

        for (String xml : xmlReports) {

            Logger.getGlobal().info(">>>>>>>>>>" + xml);
            TestSuiteModel ts = (TestSuiteModel) unm.unmarshal(new File(xml));
            for (TestCaseModel tc : ts.getTestcase()) {
                if (tc.getError() != null) {
                    System.out.println();
                }
            }
            ts.postProcess();
            for (TestCaseModel tc : ts.getTestcase()) {
                tc.postProcess();
            }
            processedTestSuites.add(ts);
        }
    }

    private void writeTestOverviewReport() throws IOException {
        Template template = new Helpers(new Handlebars()).registerHelpers().compile(TEST_OVERVIEW_REPORT);
        AllJUnitReports allFeatures = new AllJUnitReports("Test suites overview", processedTestSuites);
        FileUtils.writeStringToFile(new File(TEST_OVERVIEW_PATH + "testSuitesOverview.html"),
            template.apply(allFeatures));
    }

    private void writeTestCaseSummaryReport() throws IOException {
        Template template = new Helpers(new Handlebars()).registerHelpers().compile(TEST_SUMMARY_REPORT);
        for (TestSuiteModel ts : processedTestSuites) {
            String content = template.apply(ts);
            FileUtils.writeStringToFile(new File(TEST_SUMMARY_PATH + ts.getUniqueID() + ".html"),
                content);
        }
    }

    private void writeTestsPassedReport() throws IOException {
        Template template = new Helpers(new Handlebars()).registerHelpers().compile(TEST_OVERVIEW_REPORT);
        Predicate<TestSuiteModel> p = new Predicate<TestSuiteModel>() {
            @Override
            public boolean test(TestSuiteModel t) {
                return t.getOverallStatus().equalsIgnoreCase(Constants.PASSED);
            }
        };
        List<TestSuiteModel> onlyPassed = cast(processedTestSuites.stream()
            .filter(p)
            .collect(Collectors.toList()));

        AllJUnitReports allTestSuites = new AllJUnitReports("Passed test suites report", onlyPassed);
        FileUtils.writeStringToFile(new File(TEST_OVERVIEW_PATH + "testsPassed.html"),
            template.apply(allTestSuites));
    }

    private void writeTestsFailedReport() throws IOException {
        Template template = new Helpers(new Handlebars()).registerHelpers().compile(TEST_OVERVIEW_REPORT);
        Predicate<TestSuiteModel> p = new Predicate<TestSuiteModel>() {
            @Override
            public boolean test(TestSuiteModel t) {
                return t.getOverallStatus().equalsIgnoreCase(Constants.FAILED);
            }
        };
        List<TestSuiteModel> onlyFailed = cast(processedTestSuites.stream()
            .filter(p)
            .collect(Collectors.toList()));

        AllJUnitReports allTestSuites = new AllJUnitReports("Failed test suites report", onlyFailed);
        FileUtils.writeStringToFile(new File(TEST_OVERVIEW_PATH + "testsFailed.html"),
            template.apply(allTestSuites));
    }

    public boolean writeReportsOnDisk() throws IOException {
        writeTestOverviewReport();
        writeTestCaseSummaryReport();
        writeTestsPassedReport();
        writeTestsFailedReport();
        for (TestSuiteModel ts : processedTestSuites) {
            if (Integer.parseInt(ts.getFailures()) > 1
                || Integer.parseInt(ts.getErrors()) > 1
                || Integer.parseInt(ts.getSkipped()) > 1) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static <T extends List< ? >> T cast(Object obj) {
        return (T) obj;
    }
}