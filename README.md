# Proof of Concept – Asterisk FastAGI Integration with ChatGPT

## Beschreibung

Dieses Proof of Concept (PoC) zeigt die Integration von Asterisk FastAGI mit ChatGPT, ergänzt durch Text-to-Speech (TTS) und Speech-to-Text (STT). 
Ziel ist es, interaktive, sprachbasierte Telefongespräche mit intelligenter Konversationsfähigkeit bereitzustellen.
Asterisk ist eine Open-Source-Softwarelösung für Telefonie, die als Private Branch Exchange (PBX) dient und Funktionen wie Anrufweiterleitung, VoIP und Konferenzgespräche bietet.


## Asterisk Konfiguration
Ergänzung des Dialplan in `extensions.conf`
```
exten => 100,1,Answer()
    same = n,AGI(agi://localhost:4573)
```