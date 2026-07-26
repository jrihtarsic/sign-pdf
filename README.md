# sign-pdf

The sign-pdf is A small Java tool for applying a **visible PAdES (PDF Advanced Electronic Signature)** to a PDF file.
It signs with a key from a PKCS12/JKS keystore, stamps a signature image plus a text block (signer
name, date, certificate issuer/serial) at a chosen spot on the page, and then checks the result: the
embedded signature is validated cryptographically, and the file's PDF/A structure is checked.

It has a small Swing desktop UI for picking the PDF, the page, and where the visible signature goes —
and a CLI entry point for scripted use.

## Screenshots

**Main window** — open a PDF, page through it, fill in signer details:

![Main window](docs/images/01-main-window.png)

**Placing a signature** — drag a rectangle on the page to mark where the visible signature goes:

![Selecting the signature area](docs/images/02-select-signature-area.png)

**Settings** — keystore, signature image, and default signer details, saved to `settings.yaml`:

![Settings dialog](docs/images/03-settings.png)


## Running

Launcher scripts are in [`etc/`](etc): [`sign-pdf.sh`](etc/sign-pdf.sh) for Linux/macOS and
[`sign-pdf.bat`](etc/sign-pdf.bat) for Windows. Both find the jar automatically — next to the script
first (drop the jar in `etc/` for a standalone deployment), otherwise `../target/` (a plain `mvn
package` checkout) — and launch the UI when run with no arguments, or the CLI when run with arguments:

```bash
# UI
etc/sign-pdf.sh
# CLI
etc/sign-pdf.sh input.pdf signed.pdf "Jane Doe" "Approval" "jane.doe@example.org" 1 50 700 200 80
```

```bat
:: UI
etc\sign-pdf.bat
:: CLI
etc\sign-pdf.bat input.pdf signed.pdf "Jane Doe" "Approval" "jane.doe@example.org" 1 50 700 200 80
```

Or run the jar directly with `java`, as shown below.

## Using the UI

```bash
java -jar target/sign-pdf-1.0-jar-with-dependencies.jar
```

1. **File → Settings...** — set the keystore path, type (PKCS12/JKS), key alias, and the signature
   image to stamp onto the page. Keystore/key passwords are optional here: leave them blank and you'll
   be prompted for them when you sign instead of storing them on disk. Settings are saved to
   `settings.yaml` next to the jar.
2. **File → Open PDF...** — pick the file to sign. Use the `< Prev` / `Next >` buttons to get to the
   right page.
3. Either **drag a rectangle** on the page to mark where the new visible signature goes, or, if the PDF
   already has an empty signature field on it, pick it from the **Signature placement** dropdown
   instead of drawing a new one.
4. Fill in **Signer name** (required), **Purpose**, and **Contact** — these default to whatever you set
   in Settings.
5. **Sign & Save...** — pick where to write the signed copy. The tool signs, then reports the
   signature's cryptographic validity and the PDF/A structural check in the log panel at the bottom.

## Using the CLI

```bash
java -cp target/sign-pdf-1.0-jar-with-dependencies.jar org.r7c.pdf.pades.PadesUtils \
  input.pdf signed.pdf "Jane Doe" "Approval" "jane.doe@example.org" 1 50 700 200 80
```

Arguments: `<fileToBeSigned> <fileSigned> signerName purpose contact page x y width height` (page is
1-based; x/y/width/height are in PDF points, origin bottom-left).

Keystore and signature-image settings come from the same `settings.yaml` the UI uses (next to the jar).
If it leaves the keystore/key password blank, set the `KEYSTORE_PASSWORD` / `KEY_PASSWORD` environment
variables before running — the CLI has no interactive prompt.

## `settings.yaml`

```yaml
keystore:
  path: /home/user/keys/company-signing.p12
  type: PKCS12
  keyAlias: company-key
  password: ""       # optional; leave blank to be prompted in the UI at sign time
  keyPassword: ""     # optional; same as above
signature:
  imagePath: /home/user/signatures/signature.png
  dateTimeFormat: "dd. MM. yyyy HH:mm"
  defaultSignerName: ""
  defaultPurpose: ""
  defaultContact: ""
output:
  suffix: "-signed"
```

## Build

### Requirements

- Java 21
- Maven (`mvn`) — there's no wrapper checked in
- Network access for the first build (dependencies resolve from Maven Central)


```bash
mvn package
```

This produces two jars in `target/`:

- `sign-pdf-1.0.jar` — plain jar, for use on a classpath you assemble yourself
- `sign-pdf-1.0-jar-with-dependencies.jar` — runnable standalone, this is the one you want


