"""Generate/verify a Noto UI subset; full user-content font is never modified.

Requires fonttools==4.60.1. Run with --verify-only as the font/wording gate.
"""
from pathlib import Path
import argparse
import json
import sys
import hashlib

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / '.codex-tmp/fonttools'))
from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.pens.recordingPen import RecordingPen

parser = argparse.ArgumentParser()
parser.add_argument('--verify-only', action='store_true')
args = parser.parse_args()
base = ROOT / 'android/app/src/main'
font_path = base / 'res/font/noto_sans_sc_variable.ttf'
output = base / 'res/font/noto_sans_sc_ui_variable.ttf'
sources = list((base / 'java/com/unfinished/app/ui').glob('*.kt'))
sources += [base / 'java/com/unfinished/app/MainActivity.kt']
characters = set(''.join(path.read_text(encoding='utf-8') for path in sources))
full = TTFont(font_path, recalcTimestamp=False)
cmap = full.getBestCmap()
# ASCII, punctuation and the source's CJK cover fixed labels, dates and period
# names. Only these two screens opt in; arbitrary user text keeps full Noto.
points = {ord(c) for c in characters if ord(c) in cmap}
points.update(cp for cp in cmap if 0x20 <= cp <= 0x7E or 0x2000 <= cp <= 0x206F or 0x3000 <= cp <= 0x303F or 0xFF00 <= cp <= 0xFFEF)
if not args.verify_only:
    options = subset.Options()
    options.layout_features = ['*']
    options.name_IDs = ['*']
    options.name_languages = ['*']
    options.glyph_names = True
    options.notdef_outline = True
    options.hinting = True
    options.recalc_timestamp = False
    worker = subset.Subsetter(options=options)
    worker.populate(unicodes=points)
    candidate = TTFont(font_path, recalcTimestamp=False)
    worker.subset(candidate)
    candidate.save(output)
    candidate.close()
    manifest = {
        'source_sha256':hashlib.sha256(font_path.read_bytes()).hexdigest(),
        'subset_sha256':hashlib.sha256(output.read_bytes()).hexdigest(),
        'source_codepoints':sorted(cmap),
        'subset_codepoints':sorted(TTFont(output).getBestCmap()),
        'generator':'fonttools==4.60.1', 'license':'SIL Open Font License 1.1'
    }
    (ROOT / 'tools/ui-font-manifest.json').write_text(json.dumps(manifest, separators=(',',':'))+'\n', encoding='utf-8')

small = TTFont(output, recalcTimestamp=False)
small_map = small.getBestCmap()
assert points <= small_map.keys(), 'New UI wording is missing glyphs; regenerate subset'
assert [(a.axisTag,a.minValue,a.defaultValue,a.maxValue) for a in full['fvar'].axes] == [(a.axisTag,a.minValue,a.defaultValue,a.maxValue) for a in small['fvar'].axes]
for table, fields in {
    'hhea': ('ascent','descent','lineGap'),
    'OS/2': ('sTypoAscender','sTypoDescender','sTypoLineGap','usWinAscent','usWinDescent'),
}.items():
    for field in fields:
        assert getattr(full[table],field) == getattr(small[table],field), (table,field)
for weight in (300,400,500,600,700):
    old_glyphs = full.getGlyphSet(location={'wght':weight})
    new_glyphs = small.getGlyphSet(location={'wght':weight})
    for cp in points:
        old, new = old_glyphs[cmap[cp]], new_glyphs[small_map[cp]]
        assert abs(old.width-new.width) < 0.001, (cp,weight,'advance')
        old_pen, new_pen = RecordingPen(), RecordingPen()
        old.draw(old_pen); new.draw(new_pen)
        assert old_pen.value == new_pen.value, (cp,weight,'outline')
result = {'source_bytes':font_path.stat().st_size,'subset_bytes':output.stat().st_size,
          'required_codepoints':len(points),'outline_weights':[300,400,500,600,700],
          'coverage':'passed','outlines_and_metrics':'identical',
          'full_user_content_font_preserved':True}
print(json.dumps(result))
