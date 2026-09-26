package pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request/response POJO for the InvenTree Company API ({@code /api/company/}).
 * A Company becomes usable as a SupplierPart's supplier only when
 * {@code isSupplier} is true.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventreeCompany {

    private Integer pk;
    private String name;
    private String currency;
    @JsonProperty("is_supplier")
    private Boolean isSupplier;
    @JsonProperty("is_manufacturer")
    private Boolean isManufacturer;
    @JsonProperty("is_customer")
    private Boolean isCustomer;

    public InventreeCompany() {
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getIsSupplier() {
        return isSupplier;
    }

    public void setIsSupplier(Boolean isSupplier) {
        this.isSupplier = isSupplier;
    }

    public Boolean getIsManufacturer() {
        return isManufacturer;
    }

    public void setIsManufacturer(Boolean isManufacturer) {
        this.isManufacturer = isManufacturer;
    }

    public Boolean getIsCustomer() {
        return isCustomer;
    }

    public void setIsCustomer(Boolean isCustomer) {
        this.isCustomer = isCustomer;
    }
}
