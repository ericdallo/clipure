{ pkgs ? import <nixpkgs> {} }:

with pkgs;

mkShell {
  buildInputs = [
    xorg.libX11
    xorg.libXext
    xorg.libXrender
    xorg.libXi
    freetype
  ];
}
