# Automatic Aerial Image Registration Tool

A proof of concept to show that it's possible to geo-reference aerial images (simple JPG) without ground control points AND without camera poses. The registration is done with a SIFT (Scale Invariant Feature Transform) between the drone aerial image and the microsoft Bing map as a reference.

## Steps are :
- Knowing more or less the area of interest, Bing reference images (geoTiff) are downloaded from microsoft server
- SIFT tie points are computed between each aerial image and the Bing image
- The projections on the map for each aerial image are computed and filtered
- Resulting image projected positions are displayed on the World-Wind globe

## Demo video :
[![launch the AIR tool](https://img.youtube.com/vi/zBxtR2VFGY4/0.jpg)](https://www.youtube.com/watch?v=zBxtR2VFGY4)
