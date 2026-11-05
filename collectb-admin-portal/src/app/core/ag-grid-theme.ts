import { colorSchemeDarkBlue, provideGlobalGridOptions, themeQuartz } from 'ag-grid-community';

/**
 * Single shared dark AG Grid theme (v36 Theming API) for the whole app. Deliberately does NOT
 * import the legacy ag-grid.css/ag-theme-quartz.css files or use the `ag-theme-quartz` CSS
 * class - mixing the Theming API with the legacy CSS themes triggers AG Grid error #239.
 */
export const agGridDarkTheme = themeQuartz.withPart(colorSchemeDarkBlue).withParams({
  accentColor: '#6366f1',
  backgroundColor: '#161b2c',
  foregroundColor: '#e2e8f0',
  chromeBackgroundColor: '#1b2136',
  headerBackgroundColor: '#1b2136',
  headerTextColor: '#94a3b8',
  headerFontWeight: 600,
  borderColor: '#2a3350',
  rowHoverColor: '#1f2740',
  selectedRowBackgroundColor: '#26305480',
  oddRowBackgroundColor: '#181e30',
  wrapperBorderRadius: 10,
  borderRadius: 6,
  fontFamily: ['Inter', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
  fontSize: 13.5,
  spacing: 8,
});

let registered = false;

/**
 * Registers {@link agGridDarkTheme} as the default for every AG Grid instance via
 * {@code provideGlobalGridOptions}. Deliberately called from each lazy-loaded grid feature
 * component (not from the eagerly-loaded app.config.ts) so importing `ag-grid-community`'s
 * Theming API stays out of the main initial bundle - keeps it inside the same lazy chunks that
 * already depend on `ag-grid-community` for `ModuleRegistry`/`ColDef`. Idempotent/cheap to call
 * more than once (each lazy chunk that uses a grid calls it), only registers once.
 */
export function registerAgGridDarkTheme(): void {
  if (registered) {
    return;
  }
  registered = true;
  provideGlobalGridOptions({ theme: agGridDarkTheme });
}
