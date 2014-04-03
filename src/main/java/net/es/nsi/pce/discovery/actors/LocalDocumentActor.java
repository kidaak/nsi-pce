/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.actors;

import akka.actor.Cancellable;
import akka.actor.UntypedActor;
import java.util.concurrent.TimeUnit;
import net.es.nsi.pce.discovery.provider.DdsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
public class LocalDocumentActor extends UntypedActor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DdsProvider provider;
    private Cancellable schedule;

    public LocalDocumentActor(DdsProvider provider) {
        this.provider = provider;
    }
    @Override
    public void preStart() {
        log.debug("preStart: entering.");  
        TimerMsg message = new TimerMsg();
        schedule = provider.getActorSystem().scheduler().scheduleOnce(Duration.create(5, TimeUnit.SECONDS), this.getSelf(), message, provider.getActorSystem().dispatcher(), null);
        log.debug("entering: exiting.");
    }

    @Override
    public void onReceive(Object msg) {
        log.debug("onReceive: entering.");
        if (msg instanceof TimerMsg) {
            TimerMsg event = (TimerMsg) msg;
            log.debug("onReceive: processing.");
            provider.loadDocuments(provider.getConfigReader().getDocuments());
            TimerMsg message = new TimerMsg();
            schedule = provider.getActorSystem().scheduler().scheduleOnce(Duration.create(provider.getConfigReader().getAuditInterval(), TimeUnit.SECONDS), this.getSelf(), message, provider.getActorSystem().dispatcher(), null);        

        } else {
            unhandled(msg);
        }
        log.debug("onReceive: exiting.");
    }
}