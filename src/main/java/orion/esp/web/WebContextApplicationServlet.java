package orion.esp.web;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.ApplicationServlet;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * {@link ApplicationServlet} that acquires {@link Application} instances from an
 * associated Spring {@link WebApplicationContext}.
 * This allows a Vaadin application to be configured normally as a Spring bean.
 *
 * <p>
 * For example, annotations such as
 * <code>{@link org.springframework.beans.factory.annotation.Autowired @Autowired}</code>,
 * <code>{@link org.springframework.beans.factory.annotation.Required @Required}</code>, etc.
 * and interfaces such as {@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware},
 * etc. will work on your {@link Application} instances.
 * </p>
 *
 * <p>
 * Important: The application bean must be declared in the associated {@link WebApplicationContext}
 * with <code>scope="session"</code>, and it must be the only instance of the configured application class.
 * </p>
 *
 * <p>
 * An example of "direct" use of this servlet in conjunction with Spring's
 * {@link org.springframework.web.context.ContextLoaderListener ContextLoaderListener}:
 * <blockquote><pre>
 *  &lt;!-- Spring context loader --&gt;
 *  &lt;listener&gt;
 *      &lt;listener-class&gt;org.springframework.web.context.ContextLoaderListener&lt;/listener-class&gt;
 *  &lt;/listener&gt;
 *
 *  &lt;!-- Vaadin servlet --&gt;
 *  &lt;servlet&gt;
 *      &lt;servlet-name&gt;myapp&lt;/servlet-name&gt;
 *      &lt;servlet-class&gt;com.example.WebContextApplicationServlet&lt;/servlet-class&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;application&lt;/param-name&gt;
 *          &lt;param-value&gt;some.spring.configured.Application&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;productionMode&lt;/param-name&gt;
 *          &lt;param-value&gt;true&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *  &lt;/servlet&gt;
 *  &lt;servlet-mapping&gt;
 *      &lt;servlet-name&gt;myapp&lt;/servlet-name&gt;
 *      &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *  &lt;/servlet-mapping&gt;
 * </pre></blockquote>
 * with this application context:
 * <blockquote><pre>
 *  &lt;!-- Activate Spring annotation support --&gt;
 *  &lt;context:annotation-config/&gt;
 *
 *  &lt;!-- Define Vaadin application bean --&gt;
 *  &lt;bean class="some.spring.configured.Application" scope="session"/&gt;
 *
 *  &lt;!-- Define other beans... --&gt;
 * </pre></blockquote>
 * </p>
 *
 * <p>
 * An example that creates a Spring MVC "controller" bean for use with Spring's
 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}:
 * <blockquote><pre>
 *  &lt;!-- Activate Spring annotation support --&gt;
 *  &lt;context:annotation-config/&gt;
 *
 *  &lt;!-- Define controller bean for Vaadin application --&gt;
 *  &lt;bean id="applicationController" class="org.springframework.web.servlet.mvc.ServletWrappingController"
 *     p:servletClass="com.example.WebContextApplicationServlet"&gt;
 *      &lt;property name="initParameters"&gt;
 *          &lt;props&gt;
 *              &lt;prop key="application"&gt;some.spring.configured.Application&lt;/prop&gt;
 *              &lt;prop key="productionMode"&gt;true&lt;/prop&gt;
 *          &lt;/props&gt;
 *      &lt;/property&gt;
 *  &lt;/bean&gt;
 *
 *  &lt;!-- Define Vaadin application bean --&gt;
 *  &lt;bean class="some.spring.configured.Application" scope="session"/&gt;
 *
 *  &lt;!-- Define other beans... --&gt;
 * </pre></blockquote>
 *
 * @see org.springframework.web.servlet.mvc.ContextLoaderListener
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.servlet.mvc.ServletWrappingController
 */
public class WebContextApplicationServlet extends ApplicationServlet {

    protected final Logger log = Logger.getLogger(getClass());

    private WebApplicationContext webApplicationContext;

    /**
     * Initialize this servlet.
     *
     * @throws ServletException if there is no {@link WebApplicationContext} associated with this servlet's context
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        log.debug("finding containing WebApplicationContext");
        try {
            this.webApplicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
        } catch (IllegalStateException e) {
            throw new ServletException("could not locate containing WebApplicationContext");
        }
    }

    /**
     * Get the containing Spring {@link WebApplicationContext}.
     * This only works after the servlet has been initialized (via {@link #init init()}).
     *
     * @throws ServletException if the operation fails
     */
    protected final WebApplicationContext getWebApplicationContext() throws ServletException {
        if (this.webApplicationContext == null)
            throw new ServletException("can't retrieve WebApplicationContext before init() is invoked");
        return this.webApplicationContext;
    }

    /**
     * Create and configure a new instance of the configured application class.
     *
     * <p>
     * The implementation in {@link WebContextApplicationServlet} invokes
     * {@link BeanFactory#createBean(Class) BeanFactory.createBean()}
     * on the associated {@link WebApplicationContext} class.
     * </p>
     *
     * @param request the triggering {@link HttpServletRequest}
     * @throws ServletException if bean creation fails
     */
    @Override
    protected Application getNewApplication(HttpServletRequest request) throws ServletException {
        Class<? extends Application> applicationClass;
        try {
            applicationClass = this.getApplicationClass();
        } catch (ClassNotFoundException e) {
            throw new ServletException("failed to create new instance of application class", e);
        }
        log.debug("acquiring new instance of " + applicationClass + " from " + this.webApplicationContext);
        try {
            return this.getWebApplicationContext().getBean(applicationClass);
        } catch (BeansException e) {
            throw new ServletException("failed to acquire new instance of " + applicationClass, e);
        }
    }
}

