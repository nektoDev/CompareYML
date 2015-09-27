package ru.nektodev.wikimart.worker;

import ru.nektodev.wikimart.model.Offer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

/**
 * Worker class to check pictures availability.
 *
 * @author Tsykin V.A.
 *         ts.slawa@gmail.com
 * @date 27.09.15
 */
public class PictureCheckWorker extends Thread {
    public static final String PICTURE_UNAVAILABLE = "p";
    private BlockingQueue<Offer> queue;
    private Integer SUCCESS_CODE = 200;


    public PictureCheckWorker(BlockingQueue<Offer> queue) {
        super();
        this.queue = queue;
    }

    /**
     * Overrided run method. Get {@link Offer} from BlockingQueue.<br/>
     * Check availability in method {@link #isExist(String)}. In case of IOException - set picture anavailable modifier </br>
     *
     * Just return if interrupted.
     */
    @Override
    public void run() {
        super.run();
        Offer offer = null;

        while(!interrupted()) {
            try {
                offer = this.queue.take();
                if (!isExist(offer.getPictureUrl())) {
                    offer.getResult().append(PICTURE_UNAVAILABLE);
                }

            } catch (InterruptedException e) {
                return;
            } catch (IOException e) {
                offer.getResult().append(PICTURE_UNAVAILABLE);
            }


        }
    }

    /**
     * Simple checking url availability. Creates HttpURLConnection and analyze HTTP_RESPONSE_CODE
     * @param urlString url to check availability
     * @return true if HTTP_RESPONSE_CODE = 200
     * @throws IOException
     */
    private boolean isExist(String urlString) throws IOException {
        URL u = new URL(urlString);

        HttpURLConnection httpURLConnection = (HttpURLConnection) u.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.connect();
        int responseCode = httpURLConnection.getResponseCode();
        httpURLConnection.disconnect();
        return SUCCESS_CODE.equals(responseCode);
    }
}
