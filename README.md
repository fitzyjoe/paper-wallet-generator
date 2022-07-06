# paper-wallet-generator
Generates a paper wallet for bitcoin

I did this project as an exercise in generating a paper wallet without using any bitcoin specific libraries.
Instead this project uses standard java for the cryptography and BouncyCastle as a crypto provider.

I included a Base58 class from Google.  Java includes a Base64 class, but not Base58.  It seems very uncommon.

Running this project creates public/private keys in Wallet Import Format (WIF), encodes them as QR codes and creates a PDF with the QR codes and the WIF as text. 
At this point it's hard coded to use my included background image (background.png), but this could be tweaked to allow a user to add an image.

When you `gradle run` this project, it will produce a wallet.pdf file.  Subsequent runs will overwrite this file.

The output file will look like this:
![alt text](https://github.com/fitzyjoe/paper-wallet-generator/blob/main/sample-output.png?raw=true)


If you use this code, you bear all the responsibility of risks that can occur with buying, selling and storing bitcoin.
I am not responsible for any losses you might experience.
