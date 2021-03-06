package br.usp.cata.web.interceptor;

import org.hibernate.Transaction;

import br.com.caelum.vraptor.Intercepts;
import br.com.caelum.vraptor.core.InterceptorStack;
import br.com.caelum.vraptor.interceptor.Interceptor;
import br.com.caelum.vraptor.ioc.RequestScoped;
import br.com.caelum.vraptor.resource.ResourceMethod;
import br.com.caelum.vraptor.util.hibernate.SessionCreator;


@RequestScoped
@Intercepts(before = LoginInterceptor.class)
public class TransactionInterceptor implements Interceptor
{
    private final SessionCreator sessionCreator;

    public TransactionInterceptor(final SessionCreator sessionCreator) {
        this.sessionCreator = sessionCreator;
    }

    public boolean accepts(final ResourceMethod method) {
        return (method.getMethod().isAnnotationPresent(Transactional.class) ||
        		method.getResource().getType().isAnnotationPresent(Transactional.class));
    }

    public void intercept(final InterceptorStack stack, final ResourceMethod method,
        final Object instance) {
        Transaction transaction = null;
        try {
            transaction = sessionCreator.getInstance().beginTransaction();

            stack.next(method, instance);

            transaction.commit();
        } catch(Exception e) {
            e.printStackTrace();
            if(transaction != null && transaction.isActive())
                transaction.rollback();
        }
    }

}
