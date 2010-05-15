./mvn-with-ld.sh assembly:assembly
fusermount -u fake
./mount.sh -db blog-import -path fake
