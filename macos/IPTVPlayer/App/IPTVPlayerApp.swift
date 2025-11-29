//
//  IPTVPlayerApp.swift
//  IPTVPlayer
//
//  Created on 2025-11-28.
//

import SwiftUI

@main
struct IPTVPlayerApp: App {
    let persistenceController = PersistenceController.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, persistenceController.container.viewContext)
        }
        .commands {
            CommandGroup(replacing: .newItem) { }
        }
    }
}
