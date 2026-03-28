# Visual Editor — Idee & Umsetzungsplan

Ziel: Im Template-Editor kann der Nutzer in der Preview auf einen gerenderten Wert klicken
(z.B. "220V") und den zugrunde liegenden UKey ändern — ohne den HTML-Code zu sehen.

---

## User Story

> Als Redakteur möchte ich in der Preview auf einen Produktwert klicken und den
> zugehörigen UKey direkt austauschen können, ohne HTML schreiben zu müssen.

---

## Technischer Ansatz

### Schritt 1 — Moonlight: Edit-Mode-Rendering

Neuer Query-Parameter `?editMode=true` am Render-Endpoint (`ProductController`).

In `RenderService.replaceSkuAttributeCalls()` werden `$skuAttr(UKEY)$`-Werte
statt als Rohwert in annotierte Spans verpackt:

```java
// Aktuell (Normal-Mode):
"<span th:text=\"${skuVoltage}\">220V</span>"

// Edit-Mode:
"<span data-illusion-ukey=\"VOLTAGE\" data-illusion-type=\"SKU\" " +
"data-illusion-index=\"0\" class=\"illusion-editable\">220V</span>"
```

Zusätzlich wird ein Highlight-CSS in den `<head>` injiziert:

```css
.illusion-editable {
  outline: 2px dashed #6366f1;
  cursor: pointer;
  border-radius: 2px;
}
.illusion-editable:hover {
  background: rgba(99,102,241,0.15);
}
```

### Schritt 2 — Summerlight: srcdoc statt src

Statt `<iframe src="http://localhost:8078/...">` wird das HTML von Summerlight
**gefetcht**, ein Click-Handler-Script injiziert und per `srcdoc` geladen.
Das vermeidet Cross-Origin-Probleme mit postMessage.

```ts
async function loadEditPreview() {
  const html = await fetch(
    `http://localhost:8078/moonlight/${country}/${language}/product-${previewSku}?page=${activePage}&editMode=true`
  ).then(r => r.text());
  iframeRef.current!.srcdoc = injectEditScript(html);
}

function injectEditScript(html: string): string {
  const script = `
    <script>
      document.querySelectorAll('[data-illusion-ukey]').forEach((el, i) => {
        el.addEventListener('click', e => {
          e.preventDefault();
          e.stopPropagation();
          window.parent.postMessage({
            type: 'illusion-ukey-click',
            ukey: el.dataset.illusionUkey,
            dtype: el.dataset.illusionType,
            index: el.dataset.illusionIndex,
            currentValue: el.textContent
          }, '*');
        });
      });
    <\/script>
  `;
  return html.replace('</body>', script + '</body>');
}
```

### Schritt 3 — Summerlight: postMessage empfangen

Im TemplateEditor einen Event-Listener auf `window.message` registrieren:

```ts
useEffect(() => {
  function onMessage(e: MessageEvent) {
    if (e.data?.type === 'illusion-ukey-click') {
      setUkeyClickPopover({
        ukey: e.data.ukey,
        dtype: e.data.dtype,
        index: Number(e.data.index),
        currentValue: e.data.currentValue,
      });
    }
  }
  window.addEventListener('message', onMessage);
  return () => window.removeEventListener('message', onMessage);
}, []);
```

Ein Popover/Drawer öffnet sich mit:
- Aktueller UKey + aktueller Wert
- Durchsuchbare UKey-Auswahl (wie beim bestehenden "📌 UKey einfügen"-Modal)
- "Übernehmen"-Button

### Schritt 4 — Template patchen & speichern

Der gewählte neue UKey ersetzt im Vorlage-HTML-Source den alten:

```ts
async function handleUkeyReplace(oldUkey: string, newUkey: string, index: number) {
  const html = await loadVorlage(activeVorlage);
  // Ersetze n-te Vorkommen von $skuAttr(OLD)$ oder $productAttr(OLD)$
  let count = 0;
  const patched = html.replace(
    /\$(skuAttr|productAttr)\(([^)]+)\)\$/g,
    (match, fn, ukey) => {
      if (ukey === oldUkey && count++ === index) {
        return `$${fn}(${newUkey})$`;
      }
      return match;
    }
  );
  await saveVorlage(activeVorlage, patched);
  loadEditPreview(); // Preview neu laden
}
```

---

## Aufwand

| Aufgabe | Aufwand |
|---|---|
| Moonlight: Edit-Mode in `replaceSkuAttributeCalls()` + CSS-Injection | ~0,5 Tage |
| Moonlight: `?editMode=true` Parameter in `ProductController` | ~0,5 Tage |
| Summerlight: srcdoc-Fetch + Script-Injection | ~1 Tag |
| Summerlight: postMessage-Empfang + UKey-Picker-Popover | ~1 Tag |
| Summerlight: Template-Source patchen + speichern + Reload | ~1 Tag |
| **Gesamt** | **~4 Tage** |

---

## Bekannte Schwierigkeiten

- **Ein UKey kommt mehrfach vor**: Gelöst durch `data-illusion-index` (0-basierter Zähler
  pro UKey beim Rendern), der beim Patchen als Selektor genutzt wird.
- **UKeys in Attributen** (`th:src`, `th:class`): `replaceSkuAttributeCalls()` muss auch
  Attribute annotieren, nicht nur Textnodes — leicht aufwändiger.
- **Kein echtes Inline-Editing**: Texte direkt im Preview tippen ist nicht das Ziel —
  der Wert kommt aus Elasticsearch, ist nicht statisch editierbar. Das Popover reicht.
- **srcdoc vs. src**: Bei srcdoc muss das HTML vollständig sein (inkl. CSS-Links);
  relative Pfade im Template müssen ggf. zu absoluten umgeschrieben werden.

---

## Erweiterungsideen (später)

- **Hover-Tooltip** zeigt UKey-Name bevor man klickt ("Hier: VOLTAGE")
- **Mehrfach-Klick-Modus**: Alle Felder auf einmal anzeigen mit Badges
- **Mapping-Direktnavigation**: Klick auf einen nicht-gemappten Wert öffnet direkt
  den Mapping-Config-Editor
- **Bild-Ersetzen**: Klick auf ein Bild öffnet UKey-Auswahl für Image-Mappings
