#!/bin/perl

$path = "header";
open(HEADER, "< $path") or die "could not open $path: $!\n";

while (<>) {
  last if m/package/;
}

$package = $_;

while (<HEADER>) {
  print;
}

print $package;

while (<>) {
  print;
}
