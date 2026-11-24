package com.classora.web;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Logger;

/** Starts the application when the web app is deployed and stops it on shutdown. */
@WebListener
public class AppListener implements ServletContextListener {

    public static final String ATTRIBUTE = "classora.application";
    private static final Logger LOG = Logger.getLogger(AppListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Application application = Application.fromEnvironment();
        event.getServletContext().setAttribute(ATTRIBUTE, application);
        LOG.info("Classora started");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object application = event.getServletContext().getAttribute(ATTRIBUTE);
        if (application instanceof Application) {
            ((Application) application).close();
        }
    }
}
