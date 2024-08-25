package lk.ijse.dep12.shared.to;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Item implements Serializable {

    private String barcode;
    private String description;
    private int qty;
    private BigDecimal price;
}
