export interface ConfigCaptcha {
  habilitado: boolean;
  siteKey: string | null;
}

export interface GreCaptchaApi {
  ready: (callback: () => void) => void;
  execute: (siteKey: string, opciones: { action: string }) => Promise<string>;
  render: (
    contenedor: string | HTMLElement,
    opciones: { sitekey: string; [clave: string]: unknown },
  ) => number;
}

declare global {
  interface Window {
    grecaptcha?: GreCaptchaApi;
  }
}
