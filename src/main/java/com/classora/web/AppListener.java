package com.classora.web;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

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
            LOG.log(Level.SEVERE, "CLASSORA STARTUP FAILURE", e);
            e.printStackTrace(System.err);
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
