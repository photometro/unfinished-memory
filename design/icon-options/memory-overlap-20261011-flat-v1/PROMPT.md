# Transparent extraction prompt set
Mode: built-in imagegen. No CLI/API fallback.

## Selected extraction
Use case: background-extraction.
Input Image 1 is the exact approved icon artwork, not a style reference.
Primary request: remove ONLY the white rounded-square tile and the black exterior surrounding it; replace both with a genuinely transparent alpha background. Keep all four colored forms pixel-faithful in their original positions, sizes, contours, overlap, and colors: the large bright-blue front organic oval, the pale-blue rear oval offset left, the large golden-yellow oval at upper right, and the smaller golden-yellow circle below it. Keep the full original square canvas and framing.
Strict invariants: no eyes, no face, no text, no new shapes. Do not recolor, shrink yellow elements, move, recenter, reshape, redraw, stylize, or change the original subtle color variation. No glass, no 3D, no highlights, no shadows, no material texture, no border, no rounded square remaining, no black background, no white halo. This is a precise transparent cutout for Android adaptive icon resource preparation, not a redesign. Preserve smooth colored edges and both yellow elements completely.

Selected output: exec-4c7f0519-0ccf-4e2e-a321-6f67eb2c1d7d.png → foreground-cutout.png.

## Discarded edge correction attempt
Use case: background-extraction. Image 1 is the approved source, Image 2 is the first cutout attempt. Produce a corrected transparent cutout from Image 1. Image 2 has jagged edge specks and slightly inflated spacing/contours; do not carry these mistakes forward. Change ONLY the background of Image 1: remove white rounded square and black outside to true transparent alpha. Keep Image 1's exact original scale, square framing, all four colored shapes, contours, centers, proportions, relative arrangement and overlap. The two blue shapes and two yellow shapes must stay as in Image 1, with no eye dots. Preserve flat bright blue and pale blue, golden yellow contrast and original very subtle color variation. The larger yellow oval and smaller yellow circle must retain their original ratio and separation; never reduce yellow prominence. Clean, smooth antialiased silhouette edges, no white/black halo and no stray pixels. No redesign, no restyling, no texture, no glass, no dimensional highlights, no shadow, no lettering, no border, no background panel.

Output exec-6c001735-6e5c-4f1b-b4ed-dc1b97ac0333.png not used; it did not improve edge fidelity.

Both default outputs remain in the original generated_images directory. Only the selected output is copied to this candidate.

