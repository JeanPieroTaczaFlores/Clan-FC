"""Servidor de desarrollo para TiendaMenos frontend.

Igual que `python -m http.server` pero envia Cache-Control: no-store
para que el navegador SIEMPRE pida los archivos frescos (evita ver
versiones viejas de html/js/json durante el desarrollo).

Uso:  python serve.py [puerto]
"""
import http.server
import socketserver
import sys

PUERTO = int(sys.argv[1]) if len(sys.argv) > 1 else 5500


class Handler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Cache-Control", "no-store, must-revalidate")
        self.send_header("Pragma", "no-cache")
        self.send_header("Expires", "0")
        super().end_headers()

    def log_message(self, fmt, *args):
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))


if __name__ == "__main__":
    with socketserver.ThreadingTCPServer(("127.0.0.1", PUERTO), Handler) as httpd:
        print(f"TiendaMenos dev server en http://127.0.0.1:{PUERTO} (sin cache)")
        httpd.serve_forever()
