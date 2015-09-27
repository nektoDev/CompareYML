package ru.nektodev.wikimart.model;

/**
 * Small model for offer. Contains:<br/>
 * <ul>
 *  <li>offer idetificator</li>
 *  <li>price</li>
 *  <li>url for picture</li>
 *  <li>flag that object with this id was founded in new file</li>
 *  <li>modificators string</li>
 * </ul>
 *
 * @author Tsykin V.A.
 *         ts.slawa@gmail.com
 * @date 27.09.15
 */
public class Offer {
    private Integer id;
    private String price;
    private String pictureUrl;
    private StringBuilder result = new StringBuilder();
    private boolean isFoundedInNewFile;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public synchronized String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public synchronized StringBuilder getResult() {
        return result;
    }

    public boolean isFoundedInNewFile() {
        return isFoundedInNewFile;
    }

    public void setIsFoundedInNewFile(boolean isFoundedInNewFile) {
        this.isFoundedInNewFile = isFoundedInNewFile;
    }

    @Override
    public String toString() {
        return "Offer{" +
                "id=" + id +
                ", price=" + price +
                ", pictureUrl='" + pictureUrl + '\'' +
                ", result=" + result.toString() +
                ", isFoundedInNewFile=" + isFoundedInNewFile +
                '}';
    }
}
