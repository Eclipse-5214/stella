<!-- workflow comment -->

## 🟢 Successfully built artifacts!

File expires: `{{ .expire | date "01 Sep 2006 20:00:00" }}`

| Name     | Link                   |
|----------|------------------------|
| Commit   | {{ .commit }}          |
| Logs     | {{ .logs | "View logs" }}       |
| Download | {{ .download | mdlink "Download" }} |
