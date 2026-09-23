#!/usr/bin/env python3
"""Reject a release missing the approved scene/audio or compatible native engine."""
import hashlib, io, pathlib, struct, sys, wave, zipfile
EXPECTED = {
    'study_background.jpg': 'd052fc01622119147949c99ce826c0e51f3a82ec9d17407534c1099cb273a69a',
    'newton_clack.wav': 'e90944e5c188382630acbba2df6c6f2dfedf1c9770fa45bfc7d28403c0bec5a7',
}
for path in map(pathlib.Path, sys.argv[1:]):
    with zipfile.ZipFile(path) as z:
        prefix = 'base/' if path.suffix == '.aab' else ''
        for name, expected in EXPECTED.items():
            data = z.read(prefix + 'assets/' + name)
            assert hashlib.sha256(data).hexdigest() == expected, (path, name)
        assert prefix + 'assets/newton_impact.mp3' not in z.namelist(), 'Obsolete long sound present'
        with wave.open(io.BytesIO(z.read(prefix + 'assets/newton_clack.wav'))) as w:
            seconds = w.getnframes() / w.getframerate()
            assert .14 <= seconds <= .15, seconds
        for abi in ('arm64-v8a', 'armeabi-v7a', 'x86_64'):
            assert prefix + 'lib/' + abi + '/libcradle_physics.so' in z.namelist(), abi
        for name in z.namelist():
            if not name.endswith('.so') or not any('/'+abi+'/' in name for abi in ('arm64-v8a','x86_64')):
                continue
            data = z.read(name)
            assert data[:5] == b'\x7fELF\x02', name
            endian = '<' if data[5] == 1 else '>'
            phoff = struct.unpack_from(endian+'Q', data, 32)[0]
            entsize, count = struct.unpack_from(endian+'HH', data, 54)
            for i in range(count):
                ph = struct.unpack_from(endian+'IIQQQQQQ', data, phoff+i*entsize)
                if ph[0] == 1:
                    assert ph[7] >= 16384, (name, 'LOAD alignment', ph[7])
        if path.suffix == '.aab':
            assert any(n.startswith('META-INF/') and n.endswith(('.RSA','.EC','.DSA')) for n in z.namelist()), 'Missing signing certificate'
    print(path.name + ': approved study + 145 ms clack, three native ABIs, 16 KB alignment')
