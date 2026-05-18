INSERT INTO usuarios (id, nombre, email, password, rol) VALUES
  (1001, 'Olivia Organizadora', 'org@demo.com',   '$2a$10$9.2Z16KkHePpsbHUmyMX.ez.hBGkuNGrs5LDABbZGa5UQtKAcEaQq', 'ORGANIZADOR'),
  (1002, 'Alice Asistente',     'alice@demo.com',  '$2a$10$9.2Z16KkHePpsbHUmyMX.ez.hBGkuNGrs5LDABbZGa5UQtKAcEaQq', 'ASISTENTE'),
  (1003, 'Bob Asistente',       'bob@demo.com',    '$2a$10$9.2Z16KkHePpsbHUmyMX.ez.hBGkuNGrs5LDABbZGa5UQtKAcEaQq', 'ASISTENTE'),
  (1004, 'Carol Asistente',     'carol@demo.com',  '$2a$10$9.2Z16KkHePpsbHUmyMX.ez.hBGkuNGrs5LDABbZGa5UQtKAcEaQq', 'ASISTENTE');

INSERT INTO eventos (id, nombre, descripcion, fecha, hora, ubicacion, aforo_maximo, entradas_vendidas, precio_base, estado, organizador_id) VALUES
  (2001, 'Concierto Jazz Bilbao',  'EarlyBird 0% de ocupacion.',   DATEADD('MONTH', 2, CURRENT_DATE), '20:00:00', 'Bilbao',          100, 0,  50.00, 'PUBLICADO', 1001),
  (2002, 'Festival Indie Donosti', 'Regular 60% de ocupacion.',    DATEADD('MONTH', 2, CURRENT_DATE), '21:00:00', 'San Sebastian',   100, 60, 80.00, 'PUBLICADO', 1001),
  (2003, 'Show Stand-Up Vitoria',  'LastMinute 90% de ocupacion.', DATEADD('MONTH', 1, CURRENT_DATE), '22:00:00', 'Vitoria',         100, 90, 30.00, 'PUBLICADO', 1001),
  (2004, 'Final Liga Esports',     'AGOTADO 100% de ocupacion.',   DATEADD('MONTH', 3, CURRENT_DATE), '19:00:00', 'Bilbao Arena',     50, 50, 25.00, 'AGOTADO',   1001),
  (2005, 'Charla Tech (Borrador)', 'Aun sin publicar (BORRADOR).', DATEADD('MONTH', 6, CURRENT_DATE), '18:00:00', 'Bilbao',          200, 0,  0.00,  'BORRADOR',  1001);

INSERT INTO tickets (id, uuid, evento_id, asistente_id, estado, precio_final, fecha_compra) VALUES
  (3001, '11111111-1111-1111-1111-111111111111', 2001, 1002, 'VALIDO',    50.00, CURRENT_TIMESTAMP),
  (3002, '22222222-2222-2222-2222-222222222222', 2002, 1002, 'VALIDO',    80.00, CURRENT_TIMESTAMP),
  (3003, '33333333-3333-3333-3333-333333333333', 2003, 1003, 'VALIDO',    45.00, CURRENT_TIMESTAMP),
  (3004, '44444444-4444-4444-4444-444444444444', 2002, 1004, 'CANCELADO', 80.00, CURRENT_TIMESTAMP);