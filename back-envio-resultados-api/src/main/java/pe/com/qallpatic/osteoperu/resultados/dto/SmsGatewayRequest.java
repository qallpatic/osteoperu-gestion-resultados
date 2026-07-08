package pe.com.qallpatic.osteoperu.resultados.dto;

import java.util.List;

/** Cuerpo que espera el gateway Textbee: destinatarios + mensaje. */
public record SmsGatewayRequest(List<String> recipients, String message) {
}
