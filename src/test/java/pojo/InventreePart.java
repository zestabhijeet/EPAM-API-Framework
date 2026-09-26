package pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request/response POJO for the InvenTree Part API ({@code /api/part/}).
 *
 * Only fields that are set are serialized (NON_NULL), so the same class
 * serves full creates, partial PATCH bodies and full PUT replacements.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventreePart {

    private Integer pk;
    private String name;
    @JsonProperty("IPN")
    private String ipn;
    private String revision;
    private String keywords;
    private String units;
    private String description;
    private String notes;
    private Boolean active;
    private Boolean locked;
    private Boolean component;
    private Boolean assembly;
    private Boolean consumable;
    private Boolean purchaseable;
    private Boolean salable;
    private Boolean trackable;
    private Boolean testable;
    private Integer category;
    @JsonProperty("default_location")
    private Integer defaultLocation;
    @JsonProperty("barcode_hash")
    private String barcodeHash;

    public InventreePart() {
    }

    public Integer getPk() {
        return pk;
    }

    public void setPk(Integer pk) {
        this.pk = pk;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpn() {
        return ipn;
    }

    public void setIpn(String ipn) {
        this.ipn = ipn;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Boolean getComponent() {
        return component;
    }

    public void setComponent(Boolean component) {
        this.component = component;
    }

    public Boolean getAssembly() {
        return assembly;
    }

    public void setAssembly(Boolean assembly) {
        this.assembly = assembly;
    }

    public Boolean getConsumable() {
        return consumable;
    }

    public void setConsumable(Boolean consumable) {
        this.consumable = consumable;
    }

    public Boolean getPurchaseable() {
        return purchaseable;
    }

    public void setPurchaseable(Boolean purchaseable) {
        this.purchaseable = purchaseable;
    }

    public Boolean getSalable() {
        return salable;
    }

    public void setSalable(Boolean salable) {
        this.salable = salable;
    }

    public Boolean getTrackable() {
        return trackable;
    }

    public void setTrackable(Boolean trackable) {
        this.trackable = trackable;
    }

    public Boolean getTestable() {
        return testable;
    }

    public void setTestable(Boolean testable) {
        this.testable = testable;
    }

    public Integer getCategory() {
        return category;
    }

    public void setCategory(Integer category) {
        this.category = category;
    }

    public Integer getDefaultLocation() {
        return defaultLocation;
    }

    public void setDefaultLocation(Integer defaultLocation) {
        this.defaultLocation = defaultLocation;
    }

    public String getBarcodeHash() {
        return barcodeHash;
    }

    public void setBarcodeHash(String barcodeHash) {
        this.barcodeHash = barcodeHash;
    }

    @Override
    public String toString() {
        return "InventreePart{" +
                "pk=" + pk +
                ", name='" + name + '\'' +
                ", IPN='" + ipn + '\'' +
                ", revision='" + revision + '\'' +
                ", active=" + active +
                ", locked=" + locked +
                ", category=" + category +
                ", defaultLocation=" + defaultLocation +
                '}';
    }
}
