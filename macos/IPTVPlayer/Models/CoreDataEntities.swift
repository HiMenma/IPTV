//
//  CoreDataEntities.swift
//  IPTVPlayer
//
//  Core Data entity classes
//

import Foundation
import CoreData

// MARK: - PlaylistEntity

@objc(PlaylistEntity)
public class PlaylistEntity: NSManagedObject {
    @NSManaged public var id: String
    @NSManaged public var name: String
    @NSManaged public var type: String
    @NSManaged public var url: String?
    @NSManaged public var createdAt: Date
    @NSManaged public var updatedAt: Date
    @NSManaged public var channels: NSSet?
    @NSManaged public var xtreamAccount: XtreamAccountEntity?
}

extension PlaylistEntity {
    @objc(addChannelsObject:)
    @NSManaged public func addToChannels(_ value: ChannelEntity)
    
    @objc(removeChannelsObject:)
    @NSManaged public func removeFromChannels(_ value: ChannelEntity)
    
    @objc(addChannels:)
    @NSManaged public func addToChannels(_ values: NSSet)
    
    @objc(removeChannels:)
    @NSManaged public func removeFromChannels(_ values: NSSet)
}

// MARK: - ChannelEntity

@objc(ChannelEntity)
public class ChannelEntity: NSManagedObject {
    @NSManaged public var id: String
    @NSManaged public var name: String
    @NSManaged public var url: String
    @NSManaged public var logoUrl: String?
    @NSManaged public var groupName: String?
    @NSManaged public var categoryId: String?
    @NSManaged public var playlist: PlaylistEntity
}

// MARK: - FavoriteEntity

@objc(FavoriteEntity)
public class FavoriteEntity: NSManagedObject {
    @NSManaged public var id: String
    @NSManaged public var channelId: String
    @NSManaged public var playlistId: String
    @NSManaged public var createdAt: Date
}

// MARK: - XtreamAccountEntity

@objc(XtreamAccountEntity)
public class XtreamAccountEntity: NSManagedObject {
    @NSManaged public var id: String
    @NSManaged public var serverUrl: String
    @NSManaged public var username: String
    @NSManaged public var password: String
    @NSManaged public var playlist: PlaylistEntity
}
