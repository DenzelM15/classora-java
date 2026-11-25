package com.classora.web;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class AppListener implements ServletContextListener {

    public static final String ATTRIBUTE = "classora.application";
    private static final Logger LOG = Logger.getLogger(AppListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            Application application = Application.fromEnvironment();
            event.getServletContext().setAttribute(ATTRIBUTE, application);
            LOG.info("Classora started");
        } catch (RuntimeException | Error e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));

            String details = writer.toString()
                    .replace("\r", "")
                    .replace("\n", " | ");

            LOG.log(Level.SEVERE, "CLASSORA STARTUP FAILURE DETAILS: " + details);
            event.getServletContext().log("CLASSORA STARTUP FAILURE DETAILS: " + details, e);

            throw e;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object application = event.getServletContext().getAttribute(ATTRIBUTE);
        if (application instanceof Application) {
            ((Application) application).close();
        }
    }
}
