package unrn.dto;

import java.math.BigDecimal;
import java.util.List;

public record CarritoCompraResponse(
        List<ItemCarritoCompraResponse> items,
        BigDecimal subtotal,
        BigDecimal descuentoAplicado,
        BigDecimal totalFinal) {
}