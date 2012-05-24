package net.i2geo.skillstextbox.client;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Image;
import net.i2geo.api.SKBServiceContext;

/** A small witness of the SKB-queries happening. */
public class WaitingSign extends Image implements SKBOracle.SKBOracleWorkListener {

    public WaitingSign(SKBServiceContext service) {
        super(service.basePath + '/' + URL_INACTIVE);
        super.addStyleName("waitingWheel");
        prefetch(service.basePath + '/' + URL_WAITING_WAIT);
        prefetch(service.basePath + '/' + URL_WAITING_WORK);
        this.basePath = service.basePath;
        this.waitingWheelToolTip=  service.getI18n().waitingWheelToolTip();
    }

    private static class State {}
    private static State STATE_INACTIVE = new State(),
        STATE_WAITING_WAIT = new State(), STATE_WAITING_WORK = new State();
    private static String URL_INACTIVE = "waiting-blank.gif",
        URL_WAITING_WAIT = "waiting-wait.gif",
        URL_WAITING_WORK = "waiting-work.gif";
    private String basePath, waitingWheelToolTip;

    private State state = STATE_INACTIVE;

    public void willLaunchRequest(int inMillis) {
        if(state!=STATE_WAITING_WAIT && state!=STATE_WAITING_WORK) {
            Timer t = new Timer() { public void run() {
                state = STATE_WAITING_WAIT;
                setUrl(basePath + '/' + URL_WAITING_WAIT);
                setTitle(waitingWheelToolTip);
            }}; t.schedule(50);
        }
    }

    public void nowLaunchedRequest() {
        if(state != STATE_WAITING_WORK) {
            Timer t = new Timer() { public void run() {
                state = STATE_WAITING_WORK;
                setUrl(basePath + '/' + URL_WAITING_WORK);
                setTitle(waitingWheelToolTip);
            }}; t.schedule(50);
        }
    }

    public void nowFinishedRequest() {
        if(state != STATE_INACTIVE) {
            state = STATE_INACTIVE;
            Timer t = new Timer() {public void run() {
                setUrl(basePath + '/' + URL_INACTIVE);
                setTitle("");
            }};
            t.schedule(200);
        }
    }

    public int getHeight() {
        return 15;
    }

    public int getWidth() {
        return 15;
    }


}
