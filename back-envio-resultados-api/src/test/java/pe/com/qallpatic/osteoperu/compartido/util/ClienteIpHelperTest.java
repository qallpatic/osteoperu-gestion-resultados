package pe.com.qallpatic.osteoperu.compartido.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClienteIpHelperTest {

	@Test
	void usaRemoteAddrResueltoPorElContenedor() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("203.0.113.25");
		request.addHeader("X-Forwarded-For", "198.51.100.99");

		assertEquals("203.0.113.25", ClienteIpHelper.resolver(request));
	}

	@Test
	void devuelveDesconocidaSinRequest() {
		assertEquals("desconocida", ClienteIpHelper.resolver(null));
	}
}
