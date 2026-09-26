package pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request/response POJO for the InvenTree SupplierPart API
 * ({@code /api/company/part/}), which links a Part to a supplying Company.
 * InvenTree only accepts a {@code supplier} whose Company record has
 * {@code is_supplier=true}, and enforces a unique (part, supplier, SKU) set.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventreeSupplierPart {

    private Integer pk;
    private Integer part;
    private Integer supplier;
    @JsonProperty("SKU")
    private String sku;

    public InventreeSupplierPart() {
    }

    public Integer getPk() {
        return pk;
    }

    public void setPk(Integer pk) {
        this.pk = pk;
    }

    public Integer getPart() {
        return part;
    }

    public void setPart(Integer part) {
        this.part = part;
    }

    public Integer getSupplier() {
        return supplier;
    }

    public void setSupplier(Integer supplier) {
        this.supplier = supplier;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
}
