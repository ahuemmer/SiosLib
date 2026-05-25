# SiosLib

This project gives you a Java library to easily access a
[SIOSLAB](https://www.ak-modul-bus.de/stat/sioslab_interface_usb_com.html) or (possibly) CompuLAB interface.

## Table of contents

<!-- toc -->
- __[Features](#features)__
- __[Usage](#usage)__
   - __[Digital input](#digital-input)__
   - __[Digital output](#digital-output)__
   - __[Analog input](#analog-input)__
   - __[Analog output](#analog-output)__
- __[docs folder](#docs-folder)__
- __[Third-Party software](#third-party-software)__
- __[Disclaimer](#disclaimer)__
- __[License](#license)__
<!-- /toc -->

## Features

- Easy to use control of the core functions of the SIOSLAB interface:
    - 8 independent digital outputs
    - 8 independent inputs
        - all of them usable as digital inputs
        - 6 of them usable as analog inputs alternatively
    - 2 more independent analog inputs
    - 2 independent analog outputs
- Robust implementation
- Using fast and nonblocking I/O with [jSerialComm](https://fazecast.github.io/jSerialComm/)
- Lightweight design

## Usage

The only class you need to access the SIOSLAB is [SiosLib](src/main/java/com/github/ahuemmer/sioslib/SiosLib.java). In
order to try and connect to a SIOSLAB attached to your PC, you can set it up like this:

```java
try(SiosLib siosLib = new SiosLib(SiosLabMode.SIOS_MODE)) {
    // ...
}
```

It's recommended to use the `SIOS_MODE` whenever possible, as it offers more functions and higher resolution analog
measurement. The alternative, `COMPULAB_MODE`, should be used especially if your device is a CompuLAB.

Constructed like this, SiosLib will scan all the serial ports of the PC (including serial ports emulated by USB) and
try to connect a SIOSLAB attached to it. You can then use the functions of the `siosLib` object as described in their
JavaDoc respectively.

### Digital input

Using `getDigitalInputValue` retrieves the combined value of the eight digital input ports. The highest bit (input 
number 7) will count as `127` and the lowest bit (input number 0) as `1`. Thus, the value can range from `0` (no input
on any input port) to `255` (input present on all ports).

### Digital output

Call `setDigitalOutputValue` with a value between `0` and `255` to set the digital outputs respectively. The logic is
the same that applies to `getDigitalInputValue`, thus, the highest output port (number 7) will be addressed by `128`,
the lowest one by `1` and any combination by the sum of their individual values.

`setDigitalOutputState` takes an array of eight booleans and sets the state of the eight digital output ports
accordingly.

Using `changeDigitalOutputState` gives you a more convenient control of the individual output ports, as you can request
any combination of `DigitalOutputState` enum contents.

To retrieve the digital last output value set, use `getDigitalOutputValue`.

### Analog input

Reading the value of an analog input can be done via `getAnalogValue`. As with `setAnalogOutputValue`, there's a rather
coarse relation between the voltage applied to the input and the value retrieved. In 10 bit mode (applicable to a
SIOSLAB device only, not to a CompuLab), the values may range from `0` to `1024`, in 8 bit mode from `0` to `255`.

### Analog output

You can (coarsely) control the voltage applied to the analog outputs by `setAnalogOutputValue`. This is possible in
`SIOS_MODE` only, as the older CompuLab does not support this functionality. Choosing a bit width of 10 bits here will
give you the possibility to discretely choose from `0` (=0V output) to `1023` (=~5V output). Using 8 bits, the control
is less fine-grained, as the values may range from `0` (=0V output) to `255` (=~5V output).

Though the values possible suggest it, please note, that the output voltage does not always exactly reflect these values
and their changes. Also, the maximum output voltage must not be exactly 5V (on my test device it was about 4.8V).

## `docs` folder

The content's of the [`docs`](docs) folder were not created by the author(s) of SiosLib. Please see the
[README](docs/README.md) there for further information.

## Third-Party software

SiosLib makes use of third-party software and libraries during the build process as well as during runtime. A
detailed list of the third-party dependencies can be found
in [`build.gradle`](./build.gradle).

Different license terms may apply to this software packages and must be considered before usage. There is no relation
between the author(s) of SiosLib and the people or companies supplying third-party software. These packages are - 
gratefully! - used within SiosLib, but not maintained, merchandised, licensed or anything else by - SiosLibs author(s).

## Disclaimer

This program is free software. It comes without any warranty, not even for merchantability or fitness for a particular
purpose.

Another disclaimer may apply to [third-party software included in SiosLib](#third-party_software), please see the
respective license models.

Please see [the License section](#license) for more details.

## License

SiosLib is licensed und the terms of the GNU Lesser General Public License (LPGL). Please
see [LICENSE.md](./LICENSE.md) for details.

Please consider the information in the ["Third-Party software"](#third-party-software) and ["Disclaimer"](#disclaimer)
sections also.
